package com.course.app.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Єдина стратегія збоїв для всіх завдань: повторний запуск з обмеженою кількістю спроб.
 * Поки лічильник повторів менший за maxRetries, кидаємо JobExecutionException(refireImmediately=true).
 * Після вичерпання спроб помилка лише логується, завдання чекає наступного запланованого запуску.
 * Нескінченного циклу немає: кількість повторів бере Quartz з context.getRefireCount().
 */
@Slf4j
public final class JobFailurePolicy {

    public static final String KEY_MAX_RETRIES = "maxRetries";

    private JobFailurePolicy() {
    }

    public static void handle(Class<? extends Job> jobClass, JobExecutionContext context,
                              Exception failure, int maxRetries) throws JobExecutionException {
        int attempt = context.getRefireCount();
        if (attempt < maxRetries) {
            log.warn("{} failed (retry {} of {}), refiring", jobClass.getSimpleName(), attempt + 1, maxRetries, failure);
            throw new JobExecutionException(failure, true);
        }
        log.error("{} failed after {} retries, giving up until the next scheduled run",
                jobClass.getSimpleName(), maxRetries, failure);
    }
}
