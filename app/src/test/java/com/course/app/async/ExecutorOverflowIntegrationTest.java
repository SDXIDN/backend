package com.course.app.async;

import com.course.app.booking.BookingDto;
import com.course.app.booking.BookingEntity;
import com.course.app.booking.BookingMapper;
import com.course.app.booking.BookingRepository;
import com.course.app.booking.confirmation.BookingConfirmationService;
import com.course.app.booking.confirmation.CalendarSyncService;
import com.course.app.booking.confirmation.DocumentService;
import com.course.app.config.AsyncConfig;
import com.course.app.error.ServiceBusyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Пул навмисно мінімальний: 1 потік і жодної черги. Перший виклик займає єдиний потік,
 * другий (календар) відхиляється -> TaskRejectedException -> ServiceBusyException (HTTP 503).
 */
@SpringJUnitConfig(classes = {
        AsyncConfig.class,
        LoggingAsyncExceptionHandler.class,
        DocumentService.class,
        CalendarSyncService.class,
        BookingConfirmationService.class
})
@TestPropertySource(properties = {
        "app.async.core-pool-size=1",
        "app.async.max-pool-size=1",
        "app.async.queue-capacity=0",
        "app.simulated-delay-ms=1000"
})
class ExecutorOverflowIntegrationTest {

    @Autowired
    private BookingConfirmationService confirmationService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    @Qualifier(AsyncConfig.BOOKING_EXECUTOR)
    private ThreadPoolTaskExecutor executor;

    @BeforeEach
    void waitUntilExecutorIdle() {
        // тести ділять один контекст: чекаємо, поки завдання попереднього тесту завершаться
        await().atMost(Duration.ofSeconds(5)).until(() -> executor.getActiveCount() == 0);
    }

    @MockitoBean
    private BookingRepository bookingRepository;

    @MockitoBean
    private BookingMapper bookingMapper;

    @Test
    void confirm_executorSaturated_throwsServiceBusyInsteadOfRawRejection() {
        BookingEntity entity = new BookingEntity();
        BookingDto dto = new BookingDto();
        dto.setId(3L);
        when(bookingRepository.findById(3L)).thenReturn(Optional.of(entity));
        when(bookingMapper.toDto(entity)).thenReturn(dto);

        ServiceBusyException ex = assertThrows(ServiceBusyException.class, () -> confirmationService.confirm(3L));

        assertThat(ex).hasCauseInstanceOf(TaskRejectedException.class);
    }

    @Test
    void asyncMethod_saturatedExecutor_rawCallThrowsTaskRejectedException() {
        BookingDto dto = new BookingDto();
        dto.setId(4L);
        documentService.generateConfirmationPdf(dto); // займає єдиний потік

        assertThrows(TaskRejectedException.class, () -> documentService.generateConfirmationPdf(dto));
    }
}
