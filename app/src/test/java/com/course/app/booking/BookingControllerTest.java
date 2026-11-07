package com.course.app.booking;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingController bookingController;

    @Test
    void getAll_serviceReturnsBookings_returnsSameList() {
        List<BookingDto> bookings = List.of(new BookingDto());
        when(bookingService.findAll()).thenReturn(bookings);

        List<BookingDto> result = bookingController.getAll();

        assertSame(bookings, result);
    }

    @Test
    void create_validRequest_delegatesToServiceAndReturnsDto() {
        CreateBookingRequest request = new CreateBookingRequest();
        BookingDto created = new BookingDto();
        created.setId(1L);
        when(bookingService.create(request)).thenReturn(created);

        BookingDto result = bookingController.create(request);

        assertEquals(1L, result.getId());
        verify(bookingService).create(request);
    }

    @Test
    void update_existingId_delegatesToServiceWithIdAndRequest() {
        CreateBookingRequest request = new CreateBookingRequest();
        BookingDto updated = new BookingDto();
        when(bookingService.update(7L, request)).thenReturn(updated);

        BookingDto result = bookingController.update(7L, request);

        assertSame(updated, result);
        verify(bookingService).update(7L, request);
    }

    @Test
    void update_unknownId_propagatesEntityNotFound() {
        CreateBookingRequest request = new CreateBookingRequest();
        when(bookingService.update(99L, request)).thenThrow(new EntityNotFoundException("missing"));

        assertThrows(EntityNotFoundException.class, () -> bookingController.update(99L, request));
    }

    @Test
    void delete_existingId_delegatesToService() {
        bookingController.delete(3L);

        verify(bookingService).delete(3L);
    }

    @Test
    void delete_unknownId_propagatesEntityNotFound() {
        org.mockito.Mockito.doThrow(new EntityNotFoundException("missing")).when(bookingService).delete(404L);

        assertThrows(EntityNotFoundException.class, () -> bookingController.delete(404L));
    }
}
