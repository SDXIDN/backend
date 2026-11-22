package com.course.app.booking.confirmation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingConfirmationControllerTest {

    @Mock
    private BookingConfirmationService confirmationService;

    @InjectMocks
    private BookingConfirmationController controller;

    @Test
    void confirm_existingBooking_delegatesToService() {
        BookingConfirmationDto dto = new BookingConfirmationDto(5L, "booking-5.pdf", 10, "evt", true, List.of());
        when(confirmationService.confirm(5L)).thenReturn(dto);

        assertSame(dto, controller.confirm(5L));
    }
}
