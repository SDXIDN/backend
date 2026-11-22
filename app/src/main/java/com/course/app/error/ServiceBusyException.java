package com.course.app.error;

/**
 * Пул потоків переповнений: клієнт має спробувати пізніше (HTTP 503 + Retry-After).
 */
public class ServiceBusyException extends RuntimeException {

    public ServiceBusyException(String message, Throwable cause) {
        super(message, cause);
    }
}
