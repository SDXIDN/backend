package com.course.app.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BookingTimeRangeValidator.class)
public @interface ValidBookingTimeRange {

    String message() default "Invalid booking time range";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    long minMinutes() default 15;

    long maxMinutes() default 480;
}
