package com.course.booking.room;

import com.course.booking.booking.BookingRepository;
import com.course.booking.common.ConflictException;
import com.course.booking.common.CrudService;
import com.course.booking.common.NotFoundException;
import com.course.booking.review.ReviewRepository;
import com.course.booking.venue.VenueRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class RoomService extends CrudService<Room> {
    private final RoomRepository roomRepository;
    private final VenueRepository venueRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;

    public RoomService(RoomRepository roomRepository, VenueRepository venueRepository,
                       BookingRepository bookingRepository, ReviewRepository reviewRepository) {
        super(roomRepository, "Room");
        this.roomRepository = roomRepository;
        this.venueRepository = venueRepository;
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
    }

    public List<Room> findAll(Long venueId) {
        return venueId == null ? findAll() : roomRepository.findByVenueId(venueId);
    }

    @Override
    protected void validate(Room room) {
        requireText(room.getName(), "name");
        requireNotNull(room.getCapacity(), "capacity");
        if (room.getCapacity() < 1) {
            throw new IllegalArgumentException("capacity must be at least 1");
        }
        requireNotNull(room.getHourlyPrice(), "hourlyPrice");
        if (room.getHourlyPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("hourlyPrice must be positive");
        }
        requireNotNull(room.getVenueId(), "venueId");
    }

    @Override
    protected void checkConstraints(Room room) {
        if (!venueRepository.existsById(room.getVenueId())) {
            throw new NotFoundException("Venue not found with id: " + room.getVenueId());
        }
    }

    @Override
    protected Room merge(Room existing, Room patch) {
        return new Room(
                orElse(patch.getName(), existing.getName()),
                orElse(patch.getCapacity(), existing.getCapacity()),
                orElse(patch.getHourlyPrice(), existing.getHourlyPrice()),
                orElse(patch.getVenueId(), existing.getVenueId()));
    }

    @Override
    protected void checkCanDelete(Long id) {
        if (bookingRepository.existsByRoomId(id) || reviewRepository.existsByRoomId(id)) {
            throw new ConflictException("Room " + id + " is referenced by bookings or reviews");
        }
    }
}
