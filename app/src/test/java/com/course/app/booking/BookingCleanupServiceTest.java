package com.course.app.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingCleanupServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-12-01T09:00:00Z"), ZoneOffset.UTC);

    @Mock
    private BookingRepository bookingRepository;

    private BookingCleanupService service;

    @BeforeEach
    void setUp() {
        service = new BookingCleanupService(bookingRepository, CLOCK);
    }

    @Test
    void cancelExpiredPending_usesThresholdFromClockAndParam() {
        LocalDateTime threshold = LocalDateTime.of(2026, 12, 1, 7, 30);
        when(bookingRepository.updateStatusForEndedBefore(BookingStatus.PENDING, BookingStatus.CANCELLED, threshold))
                .thenReturn(3);

        int cancelled = service.cancelExpiredPending(90);

        assertThat(cancelled).isEqualTo(3);
        verify(bookingRepository).updateStatusForEndedBefore(BookingStatus.PENDING, BookingStatus.CANCELLED, threshold);
    }

    @Test
    void cancelExpiredPending_calledTwice_secondRunChangesNothing() {
        // перший виклик "оновлює" 2 записи, другий уже не знаходить PENDING-записів, що прострочені
        when(bookingRepository.updateStatusForEndedBefore(any(), any(), any())).thenReturn(2, 0);

        assertThat(service.cancelExpiredPending(60)).isEqualTo(2);
        assertThat(service.cancelExpiredPending(60)).isZero();
    }
}
