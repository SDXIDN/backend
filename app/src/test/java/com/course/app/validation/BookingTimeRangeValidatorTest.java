package com.course.app.validation;

import com.course.app.booking.CreateBookingRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingTimeRangeValidatorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static CreateBookingRequest request(LocalDateTime start, LocalDateTime end) {
        CreateBookingRequest r = new CreateBookingRequest();
        r.setTitle("Haircut");
        r.setCustomerEmail("john@example.com");
        r.setStartTime(start);
        r.setEndTime(end);
        return r;
    }

    private static List<ConstraintViolation<CreateBookingRequest>> timeRangeViolations(CreateBookingRequest r) {
        Set<ConstraintViolation<CreateBookingRequest>> all = validator.validate(r);
        return all.stream()
                .filter(v -> v.getConstraintDescriptor().getAnnotation() instanceof ValidBookingTimeRange)
                .toList();
    }

    @Test
    void annotation_declaresValidatorViaConstraintMeta() {
        Constraint meta = ValidBookingTimeRange.class.getAnnotation(Constraint.class);

        assertEquals(1, meta.validatedBy().length);
        assertEquals(BookingTimeRangeValidator.class, meta.validatedBy()[0]);
    }

    @Test
    void validate_invalidRange_validatorIsDiscoveredAndInvokedAutomatically() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 10, 10, 0);

        assertEquals(1, timeRangeViolations(request(start, start.minusMinutes(5))).size());
    }

    @Test
    void validate_validRange_noViolation() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 10, 10, 0);

        assertTrue(timeRangeViolations(request(start, start.plusHours(1))).isEmpty());
        assertTrue(validator.validate(request(start, start.plusHours(1))).isEmpty());
    }

    @Test
    void validate_exactlyMinAndMaxDuration_noViolation() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 10, 8, 0);

        assertTrue(timeRangeViolations(request(start, start.plusMinutes(15))).isEmpty());
        assertTrue(timeRangeViolations(request(start, start.plusMinutes(480))).isEmpty());
    }

    @Test
    void validate_nullTimes_skippedByThisConstraint() {
        assertTrue(timeRangeViolations(request(null, null)).isEmpty());
    }

    @Test
    void validate_endBeforeStart_violationExplainsOrder() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 10, 10, 0);

        List<ConstraintViolation<CreateBookingRequest>> v = timeRangeViolations(request(start, start.minusHours(1)));

        assertEquals(1, v.size());
        assertEquals("endTime must be after startTime", v.get(0).getMessage());
        assertEquals("endTime", v.get(0).getPropertyPath().toString());
    }

    @Test
    void validate_equalStartAndEnd_violation() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 10, 10, 0);

        assertEquals("endTime must be after startTime",
                timeRangeViolations(request(start, start)).get(0).getMessage());
    }

    @Test
    void validate_tooShort_violationMentionsMinimum() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 10, 10, 0);

        String message = timeRangeViolations(request(start, start.plusMinutes(5))).get(0).getMessage();

        assertEquals("booking is too short: 5 min, minimum is 15 min", message);
    }

    @Test
    void validate_tooLong_violationMentionsMaximum() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 10, 0, 0);

        String message = timeRangeViolations(request(start, start.plusMinutes(481))).get(0).getMessage();

        assertEquals("booking is too long: 481 min, maximum is 480 min", message);
    }

    @Test
    void validate_crossesMidnight_violationMentionsSameDay() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 10, 22, 0);

        String message = timeRangeViolations(request(start, start.plusHours(4))).get(0).getMessage();

        assertEquals("booking must start and end on the same day", message);
    }

    @Test
    void validate_anyViolation_messageIsNotGeneric() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 10, 10, 0);

        String message = timeRangeViolations(request(start, start.minusMinutes(1))).get(0).getMessage();

        assertTrue(!message.equals("Invalid booking time range") && !message.toLowerCase().contains("invalid"));
    }
}
