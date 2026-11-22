package com.course.app.scheduling;

import com.course.app.booking.BookingCleanupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingCleanupJobTest {

    @Mock
    private BookingCleanupService cleanupService;

    @Mock
    private JobExecutionContext context;

    @InjectMocks
    private BookingCleanupJob job;

    @Test
    void execute_validParams_makesExactlyOneServiceCall() throws Exception {
        when(context.getMergedJobDataMap()).thenReturn(params(90));

        job.execute(context);

        verify(cleanupService).cancelExpiredPending(90);
        verifyNoMoreInteractions(cleanupService);
    }

    @Test
    void execute_missingKey_failsExplicitly() {
        JobDataMap data = new JobDataMap();
        data.put(JobFailurePolicy.KEY_MAX_RETRIES, 2);
        when(context.getMergedJobDataMap()).thenReturn(data);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> job.execute(context));

        assertThat(ex).hasMessageContaining(BookingCleanupJob.KEY_EXPIRE_AFTER_MINUTES);
        verifyNoInteractions(cleanupService);
    }

    @Test
    void execute_serviceFails_requestsLimitedRefire() {
        when(context.getMergedJobDataMap()).thenReturn(params(60));
        when(context.getRefireCount()).thenReturn(1);
        doThrow(new IllegalStateException("db down")).when(cleanupService).cancelExpiredPending(60);

        JobExecutionException ex = assertThrows(JobExecutionException.class, () -> job.execute(context));

        assertThat(ex.refireImmediately()).isTrue();
    }

    private JobDataMap params(int expireAfterMinutes) {
        JobDataMap data = new JobDataMap();
        data.put(BookingCleanupJob.KEY_EXPIRE_AFTER_MINUTES, expireAfterMinutes);
        data.put(JobFailurePolicy.KEY_MAX_RETRIES, 2);
        return data;
    }
}
