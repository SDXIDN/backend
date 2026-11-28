package com.course.booking.room;

import com.course.booking.common.InMemoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

@Repository
public class RoomRepository extends InMemoryRepository<Room> {
    public List<Room> findByVenueId(Long venueId) {
        return findAll().stream().filter(r -> Objects.equals(r.getVenueId(), venueId)).toList();
    }

    public boolean existsByVenueId(Long venueId) {
        return findAll().stream().anyMatch(r -> Objects.equals(r.getVenueId(), venueId));
    }
}
