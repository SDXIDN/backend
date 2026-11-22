package com.course.app.config;

import com.course.app.async.LoggingAsyncExceptionHandler;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    public static final String BOOKING_EXECUTOR = "bookingTaskExecutor";

    private final LoggingAsyncExceptionHandler exceptionHandler;

    public AsyncConfig(LoggingAsyncExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    /**
     * Явно налаштований пул: обмежені розміри, обмежена черга, власний префікс потоків.
     * Політика відхилення — AbortPolicy: при переповненні кидається TaskRejectedException,
     * яку обробляє код, що викликає (див. BookingConfirmationService / GlobalExceptionHandler).
     * Зверніть увагу: потоки понад core створюються лише коли заповнена ЧЕРГА.
     */
    @Bean(name = BOOKING_EXECUTOR)
    public ThreadPoolTaskExecutor bookingTaskExecutor(
            @Value("${app.async.core-pool-size:2}") int corePoolSize,
            @Value("${app.async.max-pool-size:4}") int maxPoolSize,
            @Value("${app.async.queue-capacity:20}") int queueCapacity,
            @Value("${app.async.thread-name-prefix:booking-async-}") String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return exceptionHandler;
    }
}
