package com.course.app.booking.confirmation;

import com.course.app.async.SimulatedLatency;
import com.course.app.booking.BookingDto;
import com.course.app.config.AsyncConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class CalendarSyncService {

    private final long delayMs;

    public CalendarSyncService(@Value("${app.simulated-delay-ms:1000}") long delayMs) {
        this.delayMs = delayMs;
    }

    /** Повільний побічний ефект №3: виклик зовнішнього календарного сервісу (~1 с). Повертає id події. */
    @Async(AsyncConfig.BOOKING_EXECUTOR)
    public CompletableFuture<String> syncToExternalCalendar(BookingDto booking) {
        SimulatedLatency.sleep(delayMs);
        return CompletableFuture.completedFuture("cal-event-" + booking.getId());
    }
}
