package com.course.app.aop;

import com.course.app.booking.BookingDto;
import org.springframework.stereotype.Component;

@Component
public class SelfInvocationDemo {

    @MaskEmail
    public BookingDto sampleBooking() {
        BookingDto dto = new BookingDto();
        dto.setId(1L);
        dto.setTitle("Demo");
        dto.setCustomerEmail("john.smith@example.com");
        return dto;
    }

    public BookingDto sampleBookingViaThis() {
        return this.sampleBooking();
    }
}
