package com.course.app.booking;

import com.course.app.notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookingCreatedListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BookingCreatedListener listener;

    @Test
    void onBookingCreated_delegatesToNotificationService() {
        BookingDto dto = new BookingDto();

        listener.onBookingCreated(new BookingCreatedEvent(dto));

        verify(notificationService).sendBookingCreatedEmail(dto);
    }

    @Test
    void onBookingCreated_executorSaturated_doesNotPropagateRejection() {
        BookingDto dto = new BookingDto();
        dto.setId(1L);
        doThrow(new TaskRejectedException("full")).when(notificationService).sendBookingCreatedEmail(dto);

        assertThatCode(() -> listener.onBookingCreated(new BookingCreatedEvent(dto))).doesNotThrowAnyException();
    }
}
