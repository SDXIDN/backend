package com.course.app.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Duration;
import java.time.LocalDateTime;

public class BookingTimeRangeValidator
        implements ConstraintValidator<ValidBookingTimeRange, TimeRangeSource> {

    private long minMinutes;
    private long maxMinutes;

    @Override
    public void initialize(ValidBookingTimeRange constraint) {
        this.minMinutes = constraint.minMinutes();
        this.maxMinutes = constraint.maxMinutes();
    }

    @Override
    public boolean isValid(TimeRangeSource value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        LocalDateTime start = value.getStartTime();
        LocalDateTime end = value.getEndTime();
        if (start == null || end == null) {
            return true;
        }

        if (!end.isAfter(start)) {
            return violation(context, "endTime must be after startTime");
        }
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes < minMinutes) {
            return violation(context,
                    "booking is too short: " + minutes + " min, minimum is " + minMinutes + " min");
        }
        if (minutes > maxMinutes) {
            return violation(context,
                    "booking is too long: " + minutes + " min, maximum is " + maxMinutes + " min");
        }
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            return violation(context, "booking must start and end on the same day");
        }
        return true;
    }

    private static boolean violation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode("endTime")
                .addConstraintViolation();
        return false;
    }
}
