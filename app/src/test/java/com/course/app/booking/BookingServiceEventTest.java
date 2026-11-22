package com.course.app.booking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceEventTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingMapper bookingMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void create_validRequest_publishesBookingCreatedEventWithSavedDto() {
        CreateBookingRequest request = new CreateBookingRequest();
        BookingEntity entity = new BookingEntity();
        BookingEntity saved = new BookingEntity();
        BookingDto dto = new BookingDto();
        when(bookingMapper.toEntity(request)).thenReturn(entity);
        when(bookingRepository.save(entity)).thenReturn(saved);
        when(bookingMapper.toDto(saved)).thenReturn(dto);

        BookingDto result = bookingService.create(request);

        assertSame(dto, result);
        verify(eventPublisher).publishEvent(new BookingCreatedEvent(dto));
    }
}
