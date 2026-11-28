package com.course.booking.venue;

import com.course.booking.common.ConflictException;
import com.course.booking.common.CrudService;
import com.course.booking.room.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VenueService extends CrudService<Venue> {
    private final VenueRepository venueRepository;
    private final RoomRepository roomRepository;

    public VenueService(VenueRepository venueRepository, RoomRepository roomRepository) {
        super(venueRepository, "Venue");
        this.venueRepository = venueRepository;
        this.roomRepository = roomRepository;
    }

    public List<Venue> findAll(String city) {
        return (city == null || city.isBlank()) ? findAll() : venueRepository.findByCity(city);
    }

    @Override
    protected void validate(Venue venue) {
        requireText(venue.getName(), "name");
        requireText(venue.getAddress(), "address");
        requireText(venue.getCity(), "city");
    }

    @Override
    protected Venue merge(Venue existing, Venue patch) {
        return new Venue(
                orElse(patch.getName(), existing.getName()),
                orElse(patch.getAddress(), existing.getAddress()),
                orElse(patch.getCity(), existing.getCity()));
    }

    @Override
    protected void checkCanDelete(Long id) {
        if (roomRepository.existsByVenueId(id)) {
            throw new ConflictException("Venue " + id + " still has rooms; delete them first");
        }
    }
}
