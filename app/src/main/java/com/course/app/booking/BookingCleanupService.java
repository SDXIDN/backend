package com.course.app.booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Скасовує бронювання, які так і не були підтверджені, а їхній час уже минув.
 * Ідемпотентно: після першого запуску PENDING-записів для скасування не лишається.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingCleanupService {

    private final BookingRepository bookingRepository;
    private final Clock clock;

    @Transactional
    public int cancelExpiredPending(int expireAfterMinutes) {
        LocalDateTime threshold = LocalDateTime.now(clock).minusMinutes(expireAfterMinutes);
        int cancelled = bookingRepository.updateStatusForEndedBefore(
                BookingStatus.PENDING, BookingStatus.CANCELLED, threshold);
        log.info("Booking cleanup: {} expired PENDING bookings cancelled", cancelled);
        return cancelled;
    }
}
