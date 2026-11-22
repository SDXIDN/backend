package com.course.app.scheduling;

import com.course.app.booking.BookingReminderService;
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
class BookingReminderJobTest {

    @Mock
    private BookingReminderService reminderService;

    @Mock
    private JobExecutionContext context;

    @InjectMocks
    private BookingReminderJob job;

    @Test
    void execute_validParams_readsThemAndMakesExactlyOneServiceCall() throws Exception {
        when(context.getMergedJobDataMap()).thenReturn(params(48, 2));

        job.execute(context);

        verify(reminderService).sendUpcomingReminders(48);
        verifyNoMoreInteractions(reminderService);
    }

    @Test
    void execute_missingKey_failsExplicitlyAndDoesNotCallService() {
        JobDataMap data = new JobDataMap();
        data.put(JobFailurePolicy.KEY_MAX_RETRIES, 2);
        when(context.getMergedJobDataMap()).thenReturn(data);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> job.execute(context));

        assertThat(ex).hasMessageContaining(BookingReminderJob.KEY_HOURS_AHEAD);
        verifyNoInteractions(reminderService);
    }

    @Test
    void execute_misspelledKey_failsExplicitly() {
        JobDataMap data = new JobDataMap();
        data.put("hoursAhed", 24); // опечатка
        data.put(JobFailurePolicy.KEY_MAX_RETRIES, 2);
        when(context.getMergedJobDataMap()).thenReturn(data);

        assertThrows(IllegalArgumentException.class, () -> job.execute(context));

        verifyNoInteractions(reminderService);
    }

    @Test
    void execute_wrongValueType_failsExplicitly() {
        JobDataMap data = new JobDataMap();
        data.put(BookingReminderJob.KEY_HOURS_AHEAD, "soon");
        data.put(JobFailurePolicy.KEY_MAX_RETRIES, 2);
        when(context.getMergedJobDataMap()).thenReturn(data);

        assertThrows(IllegalArgumentException.class, () -> job.execute(context));
    }

    @Test
    void execute_serviceFails_requestsLimitedRefire() {
        when(context.getMergedJobDataMap()).thenReturn(params(24, 2));
        when(context.getRefireCount()).thenReturn(0);
        doThrow(new IllegalStateException("db down")).when(reminderService).sendUpcomingReminders(24);

        JobExecutionException ex = assertThrows(JobExecutionException.class, () -> job.execute(context));

        assertThat(ex.refireImmediately()).isTrue();
    }

    @Test
    void execute_serviceFailsAndRetriesExhausted_doesNotRefire() throws Exception {
        when(context.getMergedJobDataMap()).thenReturn(params(24, 2));
        when(context.getRefireCount()).thenReturn(2);
        doThrow(new IllegalStateException("db down")).when(reminderService).sendUpcomingReminders(24);

        job.execute(context); // не кидає: помилка зафіксована, чекаємо наступного запуску

        verify(reminderService).sendUpcomingReminders(24);
    }

    private JobDataMap params(int hoursAhead, int maxRetries) {
        JobDataMap data = new JobDataMap();
        data.put(BookingReminderJob.KEY_HOURS_AHEAD, hoursAhead);
        data.put(JobFailurePolicy.KEY_MAX_RETRIES, maxRetries);
        return data;
    }
}
