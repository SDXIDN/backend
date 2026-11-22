package com.course.app.scheduling;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Усі завдання (JobDetail) і тригери оголошені тут, в одному місці.
 * Spring Boot сам реєструє всі біни JobDetail та Trigger у планувальнику (JDBC job store).
 *
 * Job 1 - BookingReminderJob: два тригери (кожні 15 хв і щодня о 08:00), другий перевизначає hoursAhead.
 * Job 2 - BookingCleanupJob: один cron-тригер щодня о 03:30.
 */
@Configuration
public class QuartzConfig {

    public static final String GROUP = "booking";
    public static final String TIME_ZONE = "Europe/Kyiv";

    public static final JobKey REMINDER_JOB_KEY = JobKey.jobKey("bookingReminderJob", GROUP);
    public static final JobKey CLEANUP_JOB_KEY = JobKey.jobKey("bookingCleanupJob", GROUP);

    public static final int DEFAULT_HOURS_AHEAD = 24;
    public static final int DEFAULT_EXPIRE_AFTER_MINUTES = 60;
    public static final int DEFAULT_MAX_RETRIES = 2;

    @Bean
    public JobDetail bookingReminderJobDetail() {
        return JobBuilder.newJob(BookingReminderJob.class)
                .withIdentity(REMINDER_JOB_KEY)
                .withDescription("Sends reminders for upcoming bookings")
                .usingJobData(BookingReminderJob.KEY_HOURS_AHEAD, DEFAULT_HOURS_AHEAD)
                .usingJobData(JobFailurePolicy.KEY_MAX_RETRIES, DEFAULT_MAX_RETRIES)
                .requestRecovery(true)
                .storeDurably()
                .build();
    }

    /** Тригер 1: кожні 15 хвилин, параметри беруться зі значень завдання за замовчуванням. */
    @Bean
    public Trigger bookingReminderFrequentTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(REMINDER_JOB_KEY)
                .withIdentity("bookingReminderFrequentTrigger", GROUP)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMinutes(15)
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .build();
    }

    /** Тригер 2: щодня о 08:00 за київським часом, перевизначає hoursAhead (48 замість 24). */
    @Bean
    public Trigger bookingReminderDailyTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(REMINDER_JOB_KEY)
                .withIdentity("bookingReminderDailyTrigger", GROUP)
                .usingJobData(BookingReminderJob.KEY_HOURS_AHEAD, 48)
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 8 * * ?")
                        .inTimeZone(TimeZone.getTimeZone(ZoneId.of(TIME_ZONE)))
                        .withMisfireHandlingInstructionFireAndProceed())
                .build();
    }

    @Bean
    public JobDetail bookingCleanupJobDetail() {
        return JobBuilder.newJob(BookingCleanupJob.class)
                .withIdentity(CLEANUP_JOB_KEY)
                .withDescription("Cancels PENDING bookings that have already ended")
                .usingJobData(BookingCleanupJob.KEY_EXPIRE_AFTER_MINUTES, DEFAULT_EXPIRE_AFTER_MINUTES)
                .usingJobData(JobFailurePolicy.KEY_MAX_RETRIES, DEFAULT_MAX_RETRIES)
                .requestRecovery(true)
                .storeDurably()
                .build();
    }

    /** Щодня о 03:30 за київським часом; при пропуску запуску нічого не наздоганяємо (наступний запуск і так прийде). */
    @Bean
    public Trigger bookingCleanupTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(CLEANUP_JOB_KEY)
                .withIdentity("bookingCleanupTrigger", GROUP)
                .withSchedule(CronScheduleBuilder.cronSchedule("0 30 3 * * ?")
                        .inTimeZone(TimeZone.getTimeZone(ZoneId.of(TIME_ZONE)))
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }
}
