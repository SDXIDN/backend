package com.course.booking.venue;

import com.course.booking.common.InMemoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class VenueRepository extends InMemoryRepository<Venue> {
    public List<Venue> findByCity(String city) {
        return findAll().stream().filter(v -> v.getCity() != null && v.getCity().equalsIgnoreCase(city)).toList();
    }
}
