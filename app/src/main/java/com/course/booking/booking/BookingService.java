package com.course.booking.booking;

import com.course.booking.common.ConflictException;
import com.course.booking.common.CrudService;
import com.course.booking.common.NotFoundException;
import com.course.booking.customer.CustomerRepository;
import com.course.booking.room.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookingService extends CrudService<Booking> {
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final CustomerRepository customerRepository;

    public BookingService(BookingRepository bookingRepository, RoomRepository roomRepository,
                          CustomerRepository customerRepository) {
        super(bookingRepository, "Booking");
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.customerRepository = customerRepository;
    }

    public List<Booking> findAll(Long roomId, Long customerId) {
        if (roomId != null) {
            return bookingRepository.findByRoomId(roomId).stream()
                    .filter(b -> customerId == null || customerId.equals(b.getCustomerId()))
                    .toList();
        }
        return customerId != null ? bookingRepository.findByCustomerId(customerId) : findAll();
    }

    @Override
    protected void applyDefaults(Booking booking) {
        if (booking.getStatus() == null) {
            booking.setStatus(BookingStatus.PENDING);
        }
    }

    @Override
    protected void validate(Booking booking) {
        requireNotNull(booking.getRoomId(), "roomId");
        requireNotNull(booking.getCustomerId(), "customerId");
        requireNotNull(booking.getStartTime(), "startTime");
        requireNotNull(booking.getEndTime(), "endTime");
        requireNotNull(booking.getStatus(), "status");
        if (!booking.getEndTime().isAfter(booking.getStartTime())) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
    }

    @Override
    protected void checkConstraints(Booking booking) {
        if (!roomRepository.existsById(booking.getRoomId())) {
            throw new NotFoundException("Room not found with id: " + booking.getRoomId());
        }
        if (!customerRepository.existsById(booking.getCustomerId())) {
            throw new NotFoundException("Customer not found with id: " + booking.getCustomerId());
        }
        if (booking.getStatus() != BookingStatus.CANCELLED
                && bookingRepository.existsOverlapping(booking.getRoomId(), booking.getStartTime(),
                booking.getEndTime(), booking.getId())) {
            throw new ConflictException("Room " + booking.getRoomId() + " is already booked for this time range");
        }
    }

    @Override
    protected Booking merge(Booking existing, Booking patch) {
        return new Booking(
                orElse(patch.getRoomId(), existing.getRoomId()),
                orElse(patch.getCustomerId(), existing.getCustomerId()),
                orElse(patch.getStartTime(), existing.getStartTime()),
                orElse(patch.getEndTime(), existing.getEndTime()),
                orElse(patch.getStatus(), existing.getStatus()));
    }
}
