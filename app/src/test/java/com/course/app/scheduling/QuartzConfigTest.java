package com.course.app.scheduling;

import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.impl.JobExecutionContextImpl;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.TriggerFiredBundle;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class QuartzConfigTest {

    private final QuartzConfig config = new QuartzConfig();

    @Test
    void triggers_pointToTheirJobsByJobKey() {
        assertThat(config.bookingReminderFrequentTrigger().getJobKey()).isEqualTo(QuartzConfig.REMINDER_JOB_KEY);
        assertThat(config.bookingReminderDailyTrigger().getJobKey()).isEqualTo(QuartzConfig.REMINDER_JOB_KEY);
        assertThat(config.bookingCleanupTrigger().getJobKey()).isEqualTo(QuartzConfig.CLEANUP_JOB_KEY);
        assertThat(config.bookingReminderJobDetail().getKey()).isEqualTo(QuartzConfig.REMINDER_JOB_KEY);
        assertThat(config.bookingCleanupJobDetail().getKey()).isEqualTo(QuartzConfig.CLEANUP_JOB_KEY);
    }

    @Test
    void jobsAndTriggers_haveExplicitNameAndGroup() {
        for (JobDetail job : new JobDetail[]{config.bookingReminderJobDetail(), config.bookingCleanupJobDetail()}) {
            assertThat(job.getKey().getName()).isNotBlank();
            assertThat(job.getKey().getGroup()).isEqualTo(QuartzConfig.GROUP);
        }
        for (Trigger trigger : new Trigger[]{config.bookingReminderFrequentTrigger(),
                config.bookingReminderDailyTrigger(), config.bookingCleanupTrigger()}) {
            assertThat(trigger.getKey().getName()).isNotBlank();
            assertThat(trigger.getKey().getGroup()).isEqualTo(QuartzConfig.GROUP);
        }
    }

    @Test
    void reminderJob_hasTwoDistinctTriggers() {
        Trigger frequent = config.bookingReminderFrequentTrigger();
        Trigger daily = config.bookingReminderDailyTrigger();

        assertThat(frequent.getKey()).isNotEqualTo(daily.getKey());
        assertThat(frequent.getJobKey()).isEqualTo(daily.getJobKey());
    }

    @Test
    void mergedJobDataMap_triggerValueWinsAndOtherKeysKeepJobDefaults() {
        JobDetail job = config.bookingReminderJobDetail();
        Trigger daily = config.bookingReminderDailyTrigger();

        JobDataMap merged = mergedLikeQuartz(job, daily);

        assertThat(job.getJobDataMap().getInt(BookingReminderJob.KEY_HOURS_AHEAD)).isEqualTo(24);
        assertThat(merged.getInt(BookingReminderJob.KEY_HOURS_AHEAD)).isEqualTo(48);
        assertThat(merged.getInt(JobFailurePolicy.KEY_MAX_RETRIES)).isEqualTo(QuartzConfig.DEFAULT_MAX_RETRIES);
    }

    @Test
    void mergedJobDataMap_triggerWithoutOverrides_usesJobDefaults() {
        JobDataMap merged = mergedLikeQuartz(config.bookingReminderJobDetail(), config.bookingReminderFrequentTrigger());

        assertThat(merged.getInt(BookingReminderJob.KEY_HOURS_AHEAD)).isEqualTo(QuartzConfig.DEFAULT_HOURS_AHEAD);
    }

    @Test
    void everyTrigger_hasExplicitMisfireInstruction() {
        assertThat(config.bookingReminderFrequentTrigger().getMisfireInstruction())
                .isEqualTo(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT);
        assertThat(config.bookingReminderDailyTrigger().getMisfireInstruction())
                .isEqualTo(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
        assertThat(config.bookingCleanupTrigger().getMisfireInstruction())
                .isEqualTo(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
    }

    @Test
    void cronTriggers_haveExplicitTimeZoneNotServerDefault() {
        CronTrigger daily = (CronTrigger) config.bookingReminderDailyTrigger();
        CronTrigger cleanup = (CronTrigger) config.bookingCleanupTrigger();

        assertThat(daily.getTimeZone().getID()).isEqualTo(QuartzConfig.TIME_ZONE);
        assertThat(cleanup.getTimeZone().getID()).isEqualTo(QuartzConfig.TIME_ZONE);
    }

    @Test
    void jobs_disallowConcurrentExecution() {
        assertThat(config.bookingReminderJobDetail().getJobClass().isAnnotationPresent(DisallowConcurrentExecution.class)).isTrue();
        assertThat(config.bookingCleanupJobDetail().getJobClass().isAnnotationPresent(DisallowConcurrentExecution.class)).isTrue();
    }

    /** Те саме об'єднання, яке робить Quartz при запуску: спочатку дані завдання, поверх них дані тригера. */
    private JobDataMap mergedLikeQuartz(JobDetail job, Trigger trigger) {
        TriggerFiredBundle bundle = new TriggerFiredBundle(
                job, (OperableTrigger) trigger, null, false, new Date(), new Date(), null, null);
        return new JobExecutionContextImpl(mock(Scheduler.class), bundle, new BookingReminderJob())
                .getMergedJobDataMap();
    }
}
