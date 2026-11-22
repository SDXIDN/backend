package com.course.app.scheduling;

import com.course.app.booking.BookingCleanupService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

@DisallowConcurrentExecution
public class BookingCleanupJob implements Job {

    public static final String KEY_EXPIRE_AFTER_MINUTES = "expireAfterMinutes";

    @Autowired
    private BookingCleanupService cleanupService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap data = context.getMergedJobDataMap();
        int expireAfterMinutes = JobParams.requireInt(data, KEY_EXPIRE_AFTER_MINUTES);
        int maxRetries = JobParams.requireInt(data, JobFailurePolicy.KEY_MAX_RETRIES);
        try {
            cleanupService.cancelExpiredPending(expireAfterMinutes);
        } catch (RuntimeException ex) {
            JobFailurePolicy.handle(BookingCleanupJob.class, context, ex, maxRetries);
        }
    }
}
