package com.course.app.booking;

import com.course.app.annotation.CreatedPost;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<BookingDto> getAll() {
        return bookingService.findAll();
    }

    @CreatedPost
    @PreAuthorize("hasRole('USER')")
    public BookingDto create(@Valid @RequestBody CreateBookingRequest request) {
        return bookingService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public BookingDto update(@PathVariable Long id, @Valid @RequestBody CreateBookingRequest request) {
        return bookingService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        bookingService.delete(id);
    }
}