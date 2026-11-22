package com.course.app.scheduling;

import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JobFailurePolicyTest {

    @Test
    void handle_retriesLeft_throwsRefireImmediately() {
        JobExecutionContext context = contextWithRefireCount(new AtomicInteger(0));

        JobExecutionException ex = assertThrows(JobExecutionException.class,
                () -> JobFailurePolicy.handle(BookingReminderJob.class, context, new IllegalStateException("db"), 2));

        assertThat(ex.refireImmediately()).isTrue();
    }

    @Test
    void handle_retriesExhausted_swallowsErrorAndWaitsForNextScheduledRun() throws Exception {
        JobExecutionContext context = contextWithRefireCount(new AtomicInteger(2));

        JobFailurePolicy.handle(BookingReminderJob.class, context, new IllegalStateException("db"), 2);
        // виняток не кинуто: Quartz не буде перезапускати завдання, чекаємо наступного тригера
    }

    @Test
    void handle_repeatedFailures_stopAfterMaxRetriesInsteadOfLoopingForever() {
        AtomicInteger refireCount = new AtomicInteger(0);
        JobExecutionContext context = contextWithRefireCount(refireCount);
        int maxRetries = 3;
        int executions = 0;

        while (true) {
            executions++;
            try {
                JobFailurePolicy.handle(BookingCleanupJob.class, context, new IllegalStateException("always fails"), maxRetries);
                break;
            } catch (JobExecutionException refire) {
                refireCount.incrementAndGet(); // так робить Quartz при refireImmediately
            }
            if (executions > 100) {
                throw new AssertionError("infinite retry loop");
            }
        }

        assertThat(executions).isEqualTo(maxRetries + 1);
    }

    private JobExecutionContext contextWithRefireCount(AtomicInteger refireCount) {
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getRefireCount()).thenAnswer(invocation -> refireCount.get());
        return context;
    }
}
