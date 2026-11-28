package com.course.booking.booking;

import com.course.booking.common.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Booking extends BaseEntity {
    private Long roomId;
    private Long customerId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BookingStatus status;
}
