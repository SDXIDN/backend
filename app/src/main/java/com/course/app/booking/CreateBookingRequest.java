package com.course.app.booking;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.course.app.validation.TimeRangeSource;
import com.course.app.validation.ValidBookingTimeRange;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@ValidBookingTimeRange
public class CreateBookingRequest implements TimeRangeSource {
    @NotBlank
    private String title;

    @NotBlank
    @Email
    private String customerEmail;

    @NotNull
    private LocalDateTime startTime;

    @NotNull
    private LocalDateTime endTime;
}