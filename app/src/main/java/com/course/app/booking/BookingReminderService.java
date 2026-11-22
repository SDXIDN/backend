package com.course.app.booking;

import com.course.app.notification.MailGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Бізнес-логіка завдання-нагадування. Транзакція проходить на рівні цього методу.
 * Ідемпотентність: після відправки бронювання позначається reminder_sent=true,
 * тому повторний запуск одразу після першого нічого не робить.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingReminderService {

    static final List<BookingStatus> REMINDABLE = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    private final BookingRepository bookingRepository;
    private final MailGateway mailGateway;
    private final Clock clock;

    @Transactional
    public int sendUpcomingReminders(int hoursAhead) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<BookingEntity> due = bookingRepository.findDueForReminder(REMINDABLE, now, now.plusHours(hoursAhead));

        int sent = 0;
        for (BookingEntity booking : due) {
            try {
                mailGateway.send(
                        booking.getCustomerEmail(),
                        "Reminder: " + booking.getTitle(),
                        "Your booking #" + booking.getId() + " starts at " + booking.getStartTime());
                booking.setReminderSent(true);
                sent++;
            } catch (RuntimeException ex) {
                // збій одного листа не відкочує вже позначені бронювання: цей лист піде при наступному запуску
                log.warn("Reminder for booking {} failed, it will be retried on the next run", booking.getId(), ex);
            }
        }
        log.info("Booking reminders: {} sent out of {} due", sent, due.size());
        return sent;
    }
}
