package com.course.app.error;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBadRequest_illegalArgument_returns400WithMessage() {
        ResponseEntity<ApiError> response = handler.handleBadRequest(new IllegalArgumentException("bad input"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("bad input", response.getBody().message());
    }

    @Test
    void handleNotFound_entityNotFound_returns404WithMessage() {
        ResponseEntity<ApiError> response = handler.handleNotFound(new EntityNotFoundException("Booking 5 missing"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Booking 5 missing", response.getBody().message());
    }

    @Test
    void handleAccessDenied_accessDenied_returns403WithGenericMessage() {
        ResponseEntity<ApiError> response = handler.handleAccessDenied(new AccessDeniedException("secret details"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("You do not have permission to perform this action", response.getBody().message());
    }

    @Test
    void handleGeneric_unexpectedException_returns500WithoutLeakingDetails() {
        ResponseEntity<ApiError> response = handler.handleGeneric(new RuntimeException("internal stack details"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Unexpected error", response.getBody().message());
    }

    @Test
    void handleValidation_twoFieldErrors_joinsThemIntoOneMessage() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "title", "must not be blank"));
        bindingResult.addError(new FieldError("request", "customerEmail", "must be a well-formed email address"));

        ResponseEntity<ApiError> response = handler.handleValidation(exception(bindingResult));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("title: must not be blank; customerEmail: must be a well-formed email address",
                response.getBody().message());
    }

    @Test
    void handleValidation_noFieldErrors_returnsDefaultMessage() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");

        ResponseEntity<ApiError> response = handler.handleValidation(exception(bindingResult));

        assertEquals("Validation failed", response.getBody().message());
    }

    private MethodArgumentNotValidException exception(BeanPropertyBindingResult bindingResult) throws Exception {
        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("dummy", String.class), 0);
        return new MethodArgumentNotValidException(parameter, bindingResult);
    }

    @SuppressWarnings("unused")
    private void dummy(String value) {
    }
}
