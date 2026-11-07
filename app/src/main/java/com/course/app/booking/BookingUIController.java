package com.course.app.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/ui/bookings")
@RequiredArgsConstructor
public class BookingUIController {

    private final BookingService bookingService;

    @GetMapping
    public String getBookingsPage(Model model) {
        model.addAttribute("bookings", bookingService.findAll());
        model.addAttribute("newBooking", new CreateBookingRequest());
        return "bookings";
    }

    @PostMapping
    public String createBooking(@ModelAttribute CreateBookingRequest request) {
        bookingService.create(request);
        return "redirect:/ui/bookings";
    }
}