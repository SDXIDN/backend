package com.course.app.booking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingUIControllerTest {

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingUIController controller;

    @Test
    void getBookingsPage_bookingsExist_putsListAndEmptyFormIntoModel() {
        List<BookingDto> bookings = List.of(new BookingDto());
        when(bookingService.findAll()).thenReturn(bookings);
        Model model = new ExtendedModelMap();

        String view = controller.getBookingsPage(model);

        assertEquals("bookings", view);
        assertSame(bookings, model.getAttribute("bookings"));
        assertInstanceOf(CreateBookingRequest.class, model.getAttribute("newBooking"));
    }

    @Test
    void getBookingsPage_serviceFails_propagatesException() {
        when(bookingService.findAll()).thenThrow(new IllegalStateException("db down"));

        assertThrows(IllegalStateException.class, () -> controller.getBookingsPage(new ExtendedModelMap()));
    }

    @Test
    void createBooking_validForm_createsBookingAndRedirects() {
        CreateBookingRequest request = new CreateBookingRequest();

        String view = controller.createBooking(request);

        assertEquals("redirect:/ui/bookings", view);
        verify(bookingService).create(request);
    }

    @Test
    void createBooking_serviceFails_propagatesException() {
        CreateBookingRequest request = new CreateBookingRequest();
        when(bookingService.create(request)).thenThrow(new IllegalStateException("db down"));

        assertThrows(IllegalStateException.class, () -> controller.createBooking(request));
    }
}
