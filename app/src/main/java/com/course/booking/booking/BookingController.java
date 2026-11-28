package com.course.booking.booking;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public List<Booking> getAll(@RequestParam(required = false) Long roomId, @RequestParam(required = false) Long customerId) {
        return bookingService.findAll(roomId, customerId);
    }

    @GetMapping("/{id}")
    public Booking getById(@PathVariable Long id) {
        return bookingService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Booking create(@RequestBody Booking body) {
        return bookingService.create(body);
    }

    @PutMapping("/{id}")
    public Booking replace(@PathVariable Long id, @RequestBody Booking body) {
        return bookingService.replace(id, body);
    }

    @PatchMapping("/{id}")
    public Booking patch(@PathVariable Long id, @RequestBody Booking body) {
        return bookingService.patch(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        bookingService.delete(id);
    }
}
