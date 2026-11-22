package com.course.app.notification;

import com.course.app.booking.BookingDto;
import com.course.app.config.AsyncConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final MailGateway mailGateway;

    /**
     * Fire-and-forget: повертає керування одразу, лист відправляється у потоці bookingTaskExecutor.
     * Помилку (void-метод) фіксує AsyncUncaughtExceptionHandler.
     */
    @Async(AsyncConfig.BOOKING_EXECUTOR)
    public void sendBookingCreatedEmail(BookingDto booking) {
        mailGateway.send(
                booking.getCustomerEmail(),
                "Booking received: " + booking.getTitle(),
                "Your booking #" + booking.getId() + " has been created and is awaiting confirmation.");
    }
}
