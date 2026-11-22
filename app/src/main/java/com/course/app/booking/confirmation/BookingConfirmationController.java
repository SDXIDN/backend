package com.course.app.booking.confirmation;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingConfirmationController {

    private final BookingConfirmationService confirmationService;

    @PostMapping("/{id}/confirmation")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public BookingConfirmationDto confirm(@PathVariable Long id) {
        return confirmationService.confirm(id);
    }
}
