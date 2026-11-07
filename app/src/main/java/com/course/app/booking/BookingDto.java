package com.course.app.booking;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookingDto {
    private Long id;
    private String title;
    private String customerEmail;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BookingStatus status;
}