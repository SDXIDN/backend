package com.course.app.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Обробник помилок для {@code @Async void}-методів: у них немає Future, тож без цього
 * обробника виняток був би лише рядком у логах від SimpleAsyncUncaughtExceptionHandler.
 * Тут помилка фіксується (лог + лічильник) і не губиться.
 */
@Slf4j
@Component
public class LoggingAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    private final AtomicLong failureCount = new AtomicLong();

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        failureCount.incrementAndGet();
        // параметри навмисно не логуємо: там можуть бути персональні дані (email)
        log.error("Uncaught exception in @Async method {}.{}",
                method.getDeclaringClass().getSimpleName(), method.getName(), ex);
    }

    public long getFailureCount() {
        return failureCount.get();
    }
}
