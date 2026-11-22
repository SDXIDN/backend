package com.course.app.async;

import com.course.app.booking.BookingDto;
import com.course.app.booking.BookingEntity;
import com.course.app.booking.BookingMapper;
import com.course.app.booking.BookingRepository;
import com.course.app.booking.confirmation.BookingConfirmationDto;
import com.course.app.booking.confirmation.BookingConfirmationService;
import com.course.app.booking.confirmation.CalendarSyncService;
import com.course.app.booking.confirmation.DocumentService;
import com.course.app.config.AsyncConfig;
import com.course.app.notification.MailGateway;
import com.course.app.notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(classes = {
        AsyncConfig.class,
        LoggingAsyncExceptionHandler.class,
        NotificationService.class,
        DocumentService.class,
        CalendarSyncService.class,
        BookingConfirmationService.class
})
@TestPropertySource(properties = "app.simulated-delay-ms=500")
class AsyncExecutionIntegrationTest {

    @Autowired
    @Qualifier(AsyncConfig.BOOKING_EXECUTOR)
    private ThreadPoolTaskExecutor executor;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private BookingConfirmationService confirmationService;

    @Autowired
    private LoggingAsyncExceptionHandler asyncExceptionHandler;

    @MockitoBean
    private MailGateway mailGateway;

    @MockitoBean
    private BookingRepository bookingRepository;

    @MockitoBean
    private BookingMapper bookingMapper;

    @Test
    void executor_isExplicitlyConfiguredWithBoundedResources() {
        assertThat(executor.getCorePoolSize()).isEqualTo(2);
        assertThat(executor.getMaxPoolSize()).isEqualTo(4);
        assertThat(executor.getThreadPoolExecutor().getQueue().remainingCapacity()).isEqualTo(20);
        assertThat(executor.getThreadNamePrefix()).isEqualTo("booking-async-");
    }

    @Test
    void sendEmail_slowGateway_returnsBeforeGatewayFinishesAndRunsInOwnExecutorThread() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();
        doAnswer(invocation -> {
            threadName.set(Thread.currentThread().getName());
            started.countDown();
            release.await(5, TimeUnit.SECONDS);
            finished.countDown();
            return null;
        }).when(mailGateway).send(anyString(), anyString(), anyString());

        notificationService.sendBookingCreatedEmail(booking(1L));

        // викликаючий код уже повернувся, а повільний метод ще не завершився
        assertThat(finished.getCount()).isEqualTo(1);
        assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(threadName.get()).startsWith("booking-async-");
        assertThat(threadName.get()).isNotEqualTo(Thread.currentThread().getName());

        release.countDown();
        assertThat(finished.await(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void confirm_twoIndependentCalls_runConcurrently() {
        BookingEntity entity = new BookingEntity();
        BookingDto dto = booking(7L);
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(entity));
        when(bookingMapper.toDto(entity)).thenReturn(dto);

        long start = System.nanoTime();
        BookingConfirmationDto result = confirmationService.confirm(7L);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        assertThat(result.complete()).isTrue();
        assertThat(result.documentName()).isEqualTo("booking-7.pdf");
        assertThat(result.calendarEventId()).isEqualTo("cal-event-7");
        // кожен виклик триває 500 мс: послідовно було б >= 1000 мс, паралельно ~500 мс
        assertThat(elapsed).isLessThan(Duration.ofMillis(900));
    }

    @Test
    void sendEmail_gatewayThrows_callerNotAffectedAndHandlerRecordsFailure() {
        doThrow(new IllegalStateException("smtp down")).when(mailGateway).send(any(), any(), any());
        long before = asyncExceptionHandler.getFailureCount();

        assertThatCode(() -> notificationService.sendBookingCreatedEmail(booking(2L))).doesNotThrowAnyException();

        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(asyncExceptionHandler.getFailureCount()).isEqualTo(before + 1));
    }

    private BookingDto booking(Long id) {
        BookingDto dto = new BookingDto();
        dto.setId(id);
        dto.setTitle("Demo");
        dto.setCustomerEmail("john@example.com");
        return dto;
    }
}
