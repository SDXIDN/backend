package com.course.app.booking;

import com.course.app.aop.Audited;
import com.course.app.aop.MaskEmail;
import com.course.app.aop.RateLimited;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;

    @MaskEmail
    @Transactional(readOnly = true)
    public List<BookingDto> findAll() {
        return bookingRepository.findAll().stream()
                .map(bookingMapper::toDto)
                .toList();
    }

    @RateLimited(maxCalls = 10, perSeconds = 60)
    @Transactional
    public BookingDto create(CreateBookingRequest request) {
        BookingEntity entity = bookingMapper.toEntity(request);
        BookingEntity saved = bookingRepository.save(entity);
        return bookingMapper.toDto(saved);
    }

    @Audited(action = "UPDATE_BOOKING")
    @Transactional
    public BookingDto update(Long id, CreateBookingRequest request) {
        BookingEntity entity = bookingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with id: " + id));

        bookingMapper.updateEntity(request, entity);

        return bookingMapper.toDto(bookingRepository.save(entity));
    }

    @Audited(action = "DELETE_BOOKING")
    @Transactional
    public void delete(Long id) {
        if (!bookingRepository.existsById(id)) {
            throw new EntityNotFoundException("Booking not found with id: " + id);
        }
        bookingRepository.deleteById(id);
    }
}