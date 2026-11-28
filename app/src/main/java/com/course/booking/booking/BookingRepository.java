package com.course.booking.booking;

import com.course.booking.common.InMemoryRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Repository
public class BookingRepository extends InMemoryRepository<Booking> {
    public List<Booking> findByRoomId(Long roomId) {
        return findAll().stream().filter(b -> Objects.equals(b.getRoomId(), roomId)).toList();
    }

    public List<Booking> findByCustomerId(Long customerId) {
        return findAll().stream().filter(b -> Objects.equals(b.getCustomerId(), customerId)).toList();
    }

    public boolean existsByRoomId(Long roomId) {
        return findAll().stream().anyMatch(b -> Objects.equals(b.getRoomId(), roomId));
    }

    public boolean existsByCustomerId(Long customerId) {
        return findAll().stream().anyMatch(b -> Objects.equals(b.getCustomerId(), customerId));
    }

    public boolean existsOverlapping(Long roomId, LocalDateTime start, LocalDateTime end, Long excludeId) {
        return findAll().stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .filter(b -> Objects.equals(b.getRoomId(), roomId))
                .filter(b -> !Objects.equals(b.getId(), excludeId))
                .anyMatch(b -> start.isBefore(b.getEndTime()) && end.isAfter(b.getStartTime()));
    }
}
