package com.course.app.booking;

import com.course.app.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Лист-підтвердження відправляється лише ПІСЛЯ успішного коміту транзакції створення бронювання.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCreatedListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCreated(BookingCreatedEvent event) {
        try {
            notificationService.sendBookingCreatedEmail(event.booking());
        } catch (TaskRejectedException ex) {
            // Рішення: лист — best-effort. Бронювання вже збережене, тому при переповненні пулу
            // не валимо запит і не блокуємо потік запиту, а лише фіксуємо пропуск у логах.
            log.warn("Email executor saturated, confirmation email for booking {} was skipped",
                    event.booking().getId());
        }
    }
}
