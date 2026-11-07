package com.course.app.error;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApiErrorTest {

    @Test
    void of_knownStatus_usesMatchingReasonPhrase() {
        ApiError error = ApiError.of(404, "missing");

        assertEquals(404, error.status());
        assertEquals("Not Found", error.error());
        assertEquals("missing", error.message());
        assertNotNull(error.timestamp());
    }

    @Test
    void of_eachMappedStatus_returnsExpectedText() {
        assertEquals("Unauthorized", ApiError.of(401, "x").error());
        assertEquals("Forbidden", ApiError.of(403, "x").error());
        assertEquals("Bad Request", ApiError.of(400, "x").error());
    }

    @Test
    void of_unmappedStatus_fallsBackToGenericError() {
        assertEquals("Error", ApiError.of(500, "boom").error());
    }
}
