package com.course.app.error;

import java.time.Instant;

public record ApiError(int status, String error, String message, Instant timestamp) {

    public static ApiError of(int status, String message) {
        return new ApiError(status, statusText(status), message, Instant.now());
    }

    private static String statusText(int status) {
        return switch (status) {
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 400 -> "Bad Request";
            case 429 -> "Too Many Requests";
            default -> "Error";
        };
    }
}
