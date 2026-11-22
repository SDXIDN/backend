package com.course.app.booking.confirmation;

import com.course.app.booking.BookingDto;
import com.course.app.booking.BookingMapper;
import com.course.app.booking.BookingRepository;
import com.course.app.error.ServiceBusyException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Запускає два НЕЗАЛЕЖНІ повільні виклики (PDF і календар) одночасно та об'єднує результати.
 * Метод навмисно не транзакційний: у потоки пулу передається DTO, а не JPA-сутність.
 */
@Slf4j
@Service
public class BookingConfirmationService {

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final DocumentService documentService;
    private final CalendarSyncService calendarSyncService;
    private final long timeoutMs;

    public BookingConfirmationService(BookingRepository bookingRepository,
                                      BookingMapper bookingMapper,
                                      DocumentService documentService,
                                      CalendarSyncService calendarSyncService,
                                      @Value("${app.async.confirmation-timeout-ms:3000}") long timeoutMs) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
        this.documentService = documentService;
        this.calendarSyncService = calendarSyncService;
        this.timeoutMs = timeoutMs;
    }

    public BookingConfirmationDto confirm(Long bookingId) {
        BookingDto booking = bookingRepository.findById(bookingId)
                .map(bookingMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with id: " + bookingId));

        CompletableFuture<GeneratedDocument> pdfFuture;
        CompletableFuture<String> calendarFuture;
        try {
            // обидва виклики стартують, не чекаючи один на одного
            pdfFuture = documentService.generateConfirmationPdf(booking);
            try {
                calendarFuture = calendarSyncService.syncToExternalCalendar(booking);
            } catch (TaskRejectedException ex) {
                pdfFuture.cancel(true);
                throw ex;
            }
        } catch (TaskRejectedException ex) {
            // Рішення при переповненні пулу: чітка відповідь "спробуйте пізніше" (HTTP 503), а не 500
            throw new ServiceBusyException("Async executor is saturated", ex);
        }

        // Кожен Future обмежений тайм-аутом (orTimeout), тому join() нижче не може зависнути.
        // Збій / тайм-аут кожного виклику перетворюється на Outcome із попередженням (handle).
        CompletableFuture<Outcome<GeneratedDocument>> pdf = guarded(pdfFuture, "PDF generation");
        CompletableFuture<Outcome<String>> calendar = guarded(calendarFuture, "Calendar sync");

        return pdf.thenCombine(calendar, (p, c) -> assemble(bookingId, p, c)).join();
    }

    private BookingConfirmationDto assemble(Long bookingId, Outcome<GeneratedDocument> pdf, Outcome<String> calendar) {
        List<String> warnings = new ArrayList<>();
        if (pdf.warning() != null) {
            warnings.add(pdf.warning());
        }
        if (calendar.warning() != null) {
            warnings.add(calendar.warning());
        }
        GeneratedDocument document = pdf.value();
        return new BookingConfirmationDto(
                bookingId,
                document == null ? null : document.fileName(),
                document == null ? null : document.content().length,
                calendar.value(),
                warnings.isEmpty(),
                List.copyOf(warnings));
    }

    private <T> CompletableFuture<Outcome<T>> guarded(CompletableFuture<T> future, String operation) {
        return future
                .orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .handle((value, ex) -> {
                    if (ex == null) {
                        return Outcome.success(value);
                    }
                    Throwable cause = (ex instanceof CompletionException && ex.getCause() != null) ? ex.getCause() : ex;
                    String reason = cause instanceof TimeoutException ? "timed out" : "failed";
                    log.warn("{} {}: {}", operation, reason, cause.toString());
                    return Outcome.failure(operation + " " + reason);
                });
    }

    private record Outcome<T>(T value, String warning) {

        static <T> Outcome<T> success(T value) {
            return new Outcome<>(value, null);
        }

        static <T> Outcome<T> failure(String warning) {
            return new Outcome<>(null, warning);
        }
    }
}
