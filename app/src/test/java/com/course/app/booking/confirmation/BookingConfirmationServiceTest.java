package com.course.app.booking.confirmation;

import com.course.app.booking.BookingDto;
import com.course.app.booking.BookingEntity;
import com.course.app.booking.BookingMapper;
import com.course.app.booking.BookingRepository;
import com.course.app.error.ServiceBusyException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingConfirmationServiceTest {

    private static final long TIMEOUT_MS = 200;

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingMapper bookingMapper;
    @Mock
    private DocumentService documentService;
    @Mock
    private CalendarSyncService calendarSyncService;

    private BookingConfirmationService service;
    private BookingDto dto;

    @BeforeEach
    void setUp() {
        service = new BookingConfirmationService(
                bookingRepository, bookingMapper, documentService, calendarSyncService, TIMEOUT_MS);
        dto = new BookingDto();
        dto.setId(1L);
        BookingEntity entity = new BookingEntity();
        lenient().when(bookingRepository.findById(1L)).thenReturn(Optional.of(entity));
        lenient().when(bookingMapper.toDto(entity)).thenReturn(dto);
    }

    @Test
    void confirm_bothCallsSucceed_combinesResults() {
        when(documentService.generateConfirmationPdf(dto)).thenReturn(
                CompletableFuture.completedFuture(new GeneratedDocument("booking-1.pdf", new byte[]{1, 2, 3})));
        when(calendarSyncService.syncToExternalCalendar(dto)).thenReturn(CompletableFuture.completedFuture("evt-1"));

        BookingConfirmationDto result = service.confirm(1L);

        assertThat(result.complete()).isTrue();
        assertThat(result.documentName()).isEqualTo("booking-1.pdf");
        assertThat(result.documentSizeBytes()).isEqualTo(3);
        assertThat(result.calendarEventId()).isEqualTo("evt-1");
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void confirm_calendarFails_returnsPartialResultWithWarningInsteadOfThrowing() {
        when(documentService.generateConfirmationPdf(dto)).thenReturn(
                CompletableFuture.completedFuture(new GeneratedDocument("booking-1.pdf", new byte[]{1})));
        when(calendarSyncService.syncToExternalCalendar(dto))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("calendar down")));

        BookingConfirmationDto result = service.confirm(1L);

        assertThat(result.complete()).isFalse();
        assertThat(result.documentName()).isEqualTo("booking-1.pdf");
        assertThat(result.calendarEventId()).isNull();
        assertThat(result.warnings()).containsExactly("Calendar sync failed");
    }

    @Test
    void confirm_pdfNeverCompletes_stopsWaitingAfterTimeout() {
        when(documentService.generateConfirmationPdf(dto)).thenReturn(new CompletableFuture<>());
        when(calendarSyncService.syncToExternalCalendar(dto)).thenReturn(CompletableFuture.completedFuture("evt-1"));

        long start = System.nanoTime();
        BookingConfirmationDto result = service.confirm(1L);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        assertThat(result.documentName()).isNull();
        assertThat(result.calendarEventId()).isEqualTo("evt-1");
        assertThat(result.warnings()).containsExactly("PDF generation timed out");
        assertThat(elapsed).isLessThan(Duration.ofSeconds(2));
    }

    @Test
    void confirm_executorRejectsSecondTask_throwsServiceBusyAndCancelsFirst() {
        CompletableFuture<GeneratedDocument> pdf = new CompletableFuture<>();
        when(documentService.generateConfirmationPdf(dto)).thenReturn(pdf);
        when(calendarSyncService.syncToExternalCalendar(dto)).thenThrow(new TaskRejectedException("full"));

        assertThrows(ServiceBusyException.class, () -> service.confirm(1L));

        assertThat(pdf.isCancelled()).isTrue();
    }

    @Test
    void confirm_unknownBooking_throwsEntityNotFound() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.confirm(99L));
    }
}
