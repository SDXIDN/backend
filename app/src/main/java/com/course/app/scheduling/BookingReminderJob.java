package com.course.app.scheduling;

import com.course.app.booking.BookingReminderService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * "Легке" завдання: читає параметри, робить один виклик сервісу, власної бізнес-логіки не має.
 * Не дозволяємо паралельний запуск, щоб два запуски не надіслали один і той самий лист двічі.
 * (Spring Boot створює Job через no-arg конструктор і далі робить autowire полів.)
 */
@DisallowConcurrentExecution
public class BookingReminderJob implements Job {

    public static final String KEY_HOURS_AHEAD = "hoursAhead";

    @Autowired
    private BookingReminderService reminderService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap data = context.getMergedJobDataMap();
        int hoursAhead = JobParams.requireInt(data, KEY_HOURS_AHEAD);
        int maxRetries = JobParams.requireInt(data, JobFailurePolicy.KEY_MAX_RETRIES);
        try {
            reminderService.sendUpcomingReminders(hoursAhead);
        } catch (RuntimeException ex) {
            JobFailurePolicy.handle(BookingReminderJob.class, context, ex, maxRetries);
        }
    }
}
