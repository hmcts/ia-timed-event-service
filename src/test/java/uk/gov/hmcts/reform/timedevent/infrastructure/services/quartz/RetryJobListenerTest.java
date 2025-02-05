package uk.gov.hmcts.reform.timedevent.infrastructure.services.quartz;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.entities.TimedEvent;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.services.SchedulerService;
import uk.gov.hmcts.reform.timedevent.infrastructure.services.DateTimeProvider;
import uk.gov.hmcts.reform.timedevent.infrastructure.services.exceptions.NonRetryableException;
import uk.gov.hmcts.reform.timedevent.infrastructure.services.exceptions.RetryableException;

@ExtendWith(MockitoExtension.class)
class RetryJobListenerTest {

    private long durationInSeconds = 30;
    private long maxRetryNumber = 5;
    private ZonedDateTime dateTime = ZonedDateTime.now();
    private String jurisdiction = "IA";
    private String caseType = "Asylum";
    private long caseId = 12345;
    private String identity = "someId";

    @Mock
    private SchedulerService quartzSchedulerService;

    @Mock
    private DateTimeProvider dateTimeProvider;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @Mock
    private JobDetail jobDetail;

    @Mock
    private JobDataMap jobDataMap;

    @BeforeEach
    public void setUp() {

        when(jobDataMap.getString("event")).thenReturn(Event.EXAMPLE.toString());
        when(jobDataMap.getLong("caseId")).thenReturn(caseId);

        when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
        when(jobDetail.getKey()).thenReturn(new JobKey(identity));
        when(jobExecutionContext.getJobDetail()).thenReturn(jobDetail);
    }

    @Test
    public void should_not_re_schedule_event_when_exception_is_null() {

        RetryJobListener retryJobListener = new RetryJobListener(durationInSeconds, maxRetryNumber, quartzSchedulerService, dateTimeProvider);

        retryJobListener.jobWasExecuted(jobExecutionContext, null);

        verifyNoInteractions(quartzSchedulerService);
        verifyNoInteractions(dateTimeProvider);
    }

    @Test
    public void should_not_re_schedule_event_when_exception_is_non_retryable() {

        RetryJobListener retryJobListener = new RetryJobListener(durationInSeconds, maxRetryNumber, quartzSchedulerService, dateTimeProvider);

        retryJobListener.jobWasExecuted(jobExecutionContext, new NonRetryableException());

        verifyNoInteractions(quartzSchedulerService);
        verifyNoInteractions(dateTimeProvider);
    }

    @Test
    public void should_not_re_schedule_event_when_exception_is_retryable_but_exceed_retries_number() {

        RetryJobListener retryJobListener = new RetryJobListener(durationInSeconds, maxRetryNumber, quartzSchedulerService, dateTimeProvider);
        when(jobDataMap.getOrDefault("attempts", 0L)).thenReturn(6L);

        retryJobListener.jobWasExecuted(jobExecutionContext, new RetryableException());

        verifyNoInteractions(quartzSchedulerService);
        verifyNoInteractions(dateTimeProvider);
    }

    @Test
    public void should_re_schedule_event_when_exception_is_retryable_and_retries_number_lower_than_max() {

        when(jobDataMap.getString("jurisdiction")).thenReturn(jurisdiction);
        when(jobDataMap.getString("caseType")).thenReturn(caseType);
        when(jobDataMap.getOrDefault("attempts", 0L)).thenReturn(0L);

        when(dateTimeProvider.now()).thenReturn(dateTime);

        RetryJobListener retryJobListener = new RetryJobListener(durationInSeconds, maxRetryNumber, quartzSchedulerService, dateTimeProvider);

        retryJobListener.jobWasExecuted(jobExecutionContext, new RetryableException());

        ArgumentCaptor<TimedEvent> timedEvent = ArgumentCaptor.forClass(TimedEvent.class);

        verify(dateTimeProvider).now();
        verify(quartzSchedulerService).reschedule(timedEvent.capture());

        assertEquals(identity, timedEvent.getValue().getId());
        assertEquals(caseId, timedEvent.getValue().getCaseId());
        assertEquals(jurisdiction, timedEvent.getValue().getJurisdiction());
        assertEquals(caseType, timedEvent.getValue().getCaseType());
        assertEquals(Event.EXAMPLE, timedEvent.getValue().getEvent());
        assertTrue(timedEvent.getValue().getScheduledDateTime()
                .isBefore(dateTime.plusSeconds(durationInSeconds + 121)));
    }
}
