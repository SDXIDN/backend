package com.course.booking.review;

import com.course.booking.common.InMemoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

@Repository
public class ReviewRepository extends InMemoryRepository<Review> {
    public List<Review> findByRoomId(Long roomId) {
        return findAll().stream().filter(r -> Objects.equals(r.getRoomId(), roomId)).toList();
    }

    public boolean existsByRoomId(Long roomId) {
        return findAll().stream().anyMatch(r -> Objects.equals(r.getRoomId(), roomId));
    }

    public boolean existsByCustomerId(Long customerId) {
        return findAll().stream().anyMatch(r -> Objects.equals(r.getCustomerId(), customerId));
    }
}
