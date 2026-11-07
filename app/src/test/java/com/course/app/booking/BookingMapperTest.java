package com.course.app.booking;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BookingMapperTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 12, 1, 10, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 12, 1, 11, 0);

    private final BookingMapper mapper = new BookingMapperImpl();

    @Test
    void toDto_fullEntity_copiesAllFields() {
        BookingEntity entity = BookingEntity.builder().id(1L).title("T").customerEmail("a@b.c")
                .startTime(START).endTime(END).status(BookingStatus.CONFIRMED).build();

        BookingDto dto = mapper.toDto(entity);

        assertEquals(1L, dto.getId());
        assertEquals("T", dto.getTitle());
        assertEquals("a@b.c", dto.getCustomerEmail());
        assertEquals(START, dto.getStartTime());
        assertEquals(END, dto.getEndTime());
        assertEquals(BookingStatus.CONFIRMED, dto.getStatus());
    }

    @Test
    void toDto_nullEntity_returnsNull() {
        assertNull(mapper.toDto(null));
    }

    @Test
    void toEntity_request_setsPendingStatusAndLeavesIdEmpty() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setTitle("T");
        request.setCustomerEmail("a@b.c");
        request.setStartTime(START);
        request.setEndTime(END);

        BookingEntity entity = mapper.toEntity(request);

        assertNull(entity.getId());
        assertEquals("T", entity.getTitle());
        assertEquals(BookingStatus.PENDING, entity.getStatus());
    }

    @Test
    void toEntity_nullRequest_returnsNull() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    void updateEntity_request_changesFieldsButKeepsIdAndStatus() {
        BookingEntity entity = BookingEntity.builder().id(7L).title("Old").customerEmail("old@b.c")
                .startTime(START).endTime(END).status(BookingStatus.CONFIRMED).build();
        CreateBookingRequest request = new CreateBookingRequest();
        request.setTitle("New");
        request.setCustomerEmail("new@b.c");
        request.setStartTime(START.plusDays(1));
        request.setEndTime(END.plusDays(1));

        mapper.updateEntity(request, entity);

        assertEquals(7L, entity.getId());
        assertEquals(BookingStatus.CONFIRMED, entity.getStatus());
        assertEquals("New", entity.getTitle());
        assertEquals("new@b.c", entity.getCustomerEmail());
        assertEquals(START.plusDays(1), entity.getStartTime());
    }

    @Test
    void updateEntity_nullRequest_leavesEntityUnchanged() {
        BookingEntity entity = BookingEntity.builder().id(7L).title("Old").build();

        mapper.updateEntity(null, entity);

        assertEquals("Old", entity.getTitle());
    }
}
