package com.course.app.booking.confirmation;

import com.course.app.async.SimulatedLatency;
import com.course.app.booking.BookingDto;
import com.course.app.config.AsyncConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Service
public class DocumentService {

    private final long delayMs;

    public DocumentService(@Value("${app.simulated-delay-ms:1000}") long delayMs) {
        this.delayMs = delayMs;
    }

    /** Повільний побічний ефект №2: генерація PDF-підтвердження (~1 с). */
    @Async(AsyncConfig.BOOKING_EXECUTOR)
    public CompletableFuture<GeneratedDocument> generateConfirmationPdf(BookingDto booking) {
        SimulatedLatency.sleep(delayMs);
        String body = "%PDF-1.4\n% Booking confirmation #" + booking.getId() + "\n% " + booking.getTitle() + "\n";
        return CompletableFuture.completedFuture(
                new GeneratedDocument("booking-" + booking.getId() + ".pdf", body.getBytes(StandardCharsets.UTF_8)));
    }
}
