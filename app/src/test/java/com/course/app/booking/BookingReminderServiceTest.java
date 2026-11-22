package com.course.app.booking;

import com.course.app.notification.MailGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingReminderServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-12-01T09:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 12, 1, 9, 0);

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private MailGateway mailGateway;

    private BookingReminderService service;

    @BeforeEach
    void setUp() {
        service = new BookingReminderService(bookingRepository, mailGateway, CLOCK);
    }

    @Test
    void sendUpcomingReminders_dueBooking_sendsEmailAndMarksReminderSent() {
        BookingEntity booking = booking(1L, "a@example.com");
        when(bookingRepository.findDueForReminder(eq(BookingReminderService.REMINDABLE), eq(NOW), eq(NOW.plusHours(24))))
                .thenReturn(List.of(booking));

        int sent = service.sendUpcomingReminders(24);

        assertThat(sent).isEqualTo(1);
        assertThat(booking.isReminderSent()).isTrue();
        verify(mailGateway).send(eq("a@example.com"), anyString(), anyString());
    }

    @Test
    void sendUpcomingReminders_calledTwiceForSameData_secondRunChangesNothing() {
        BookingEntity booking = booking(1L, "a@example.com");
        // імітація БД: запит повертає лише бронювання без reminder_sent
        when(bookingRepository.findDueForReminder(any(), any(), any()))
                .thenAnswer(invocation -> booking.isReminderSent() ? List.of() : List.of(booking));

        int first = service.sendUpcomingReminders(24);
        int second = service.sendUpcomingReminders(24);

        assertThat(first).isEqualTo(1);
        assertThat(second).isZero();
        verify(mailGateway, times(1)).send(anyString(), anyString(), anyString());
    }

    @Test
    void sendUpcomingReminders_oneEmailFails_othersStillProcessedAndFailedOneStaysUnmarked() {
        BookingEntity broken = booking(1L, "broken@example.com");
        BookingEntity healthy = booking(2L, "ok@example.com");
        when(bookingRepository.findDueForReminder(any(), any(), any())).thenReturn(List.of(broken, healthy));
        doThrow(new IllegalStateException("smtp down")).when(mailGateway)
                .send(eq("broken@example.com"), anyString(), anyString());

        int sent = service.sendUpcomingReminders(24);

        assertThat(sent).isEqualTo(1);
        assertThat(broken.isReminderSent()).isFalse();
        assertThat(healthy.isReminderSent()).isTrue();
    }

    private BookingEntity booking(Long id, String email) {
        return BookingEntity.builder()
                .id(id).title("Demo").customerEmail(email)
                .startTime(NOW.plusHours(3)).endTime(NOW.plusHours(4))
                .status(BookingStatus.PENDING)
                .build();
    }
}
