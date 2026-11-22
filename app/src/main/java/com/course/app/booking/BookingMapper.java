package com.course.app.booking;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    BookingDto toDto(BookingEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "reminderSent", ignore = true)
    BookingEntity toEntity(CreateBookingRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "reminderSent", ignore = true)
    void updateEntity(CreateBookingRequest request, @MappingTarget BookingEntity entity);
}