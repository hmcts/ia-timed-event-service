package uk.gov.hmcts.reform.timedevent.infrastructure.services.quartz;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.*;
import uk.gov.hmcts.reform.timedevent.domain.entities.TimedEvent;
import uk.gov.hmcts.reform.timedevent.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.timedevent.infrastructure.services.IdentityProvider;
import uk.gov.hmcts.reform.timedevent.infrastructure.services.exceptions.SchedulerProcessingException;

@ExtendWith(MockitoExtension.class)
class QuartzSchedulerServiceTest {

    @Mock
    private Scheduler scheduler;

    @Mock
    private IdentityProvider identityProvider;

    @Mock
    private ExistingScheduledJobFinder existingScheduledJobFinder;

    private QuartzSchedulerService schedulerService;

    private final String identity = "someIdentity";
    private final String jurisdiction = "IA";
    private final String caseType = "Asylum";
    private final ZonedDateTime scheduledDateTime = ZonedDateTime.now();
    private final long caseId = 12345;

    @BeforeEach
    void setUp() {
        schedulerService = new QuartzSchedulerService(scheduler, identityProvider, existingScheduledJobFinder);
    }

    @Test
    public void should_schedule_event_if_does_not_already_exist() throws SchedulerException {

        when(identityProvider.identity()).thenReturn(identity);

        TimedEvent timedEvent = new TimedEvent(
            "",
            Event.EXAMPLE,
            scheduledDateTime,
            jurisdiction,
            caseType,
            caseId
        );

        when(existingScheduledJobFinder.getExistingSaveNotificationsToDataScheduledJob(timedEvent))
                .thenReturn(Optional.empty());

        assertEquals(identity, schedulerService.schedule(timedEvent));

        ArgumentCaptor<Trigger> trigger = ArgumentCaptor.forClass(Trigger.class);

        verify(scheduler).scheduleJob(any(JobDetail.class), trigger.capture());
        assertEquals(new TriggerKey(identity), trigger.getValue().getKey());
        assertEquals(Date.from(scheduledDateTime.toInstant()), trigger.getValue().getStartTime());
        assertEquals(new JobKey(identity), trigger.getValue().getJobKey());
        assertEquals(Event.EXAMPLE.toString(), trigger.getValue().getJobDataMap().getString("event"));
        assertEquals(jurisdiction, trigger.getValue().getJobDataMap().get("jurisdiction"));
        assertEquals(caseType, trigger.getValue().getJobDataMap().getString("caseType"));
        assertEquals(caseId, trigger.getValue().getJobDataMap().getLong("caseId"));
    }

    @Test
    public void should_not_schedule_event_if_already_exists() throws SchedulerException {

        TimedEvent timedEvent = new TimedEvent(
            "",
            Event.EXAMPLE,
            scheduledDateTime,
            jurisdiction,
            caseType,
            caseId
        );

        when(existingScheduledJobFinder.getExistingSaveNotificationsToDataScheduledJob(timedEvent))
                .thenReturn(Optional.of("eventid"));

        assertEquals("eventid", schedulerService.schedule(timedEvent));

        verifyNoInteractions(scheduler);
    }

    @Test
    public void should_re_schedule_event() throws SchedulerException {

        TimedEvent timedEvent = new TimedEvent(
            identity,
            Event.EXAMPLE,
            scheduledDateTime,
            jurisdiction,
            caseType,
            caseId
        );

        assertEquals(identity, schedulerService.reschedule(timedEvent));

        ArgumentCaptor<Trigger> trigger = ArgumentCaptor.forClass(Trigger.class);

        verify(scheduler).rescheduleJob(eq(new TriggerKey(identity)), trigger.capture());

        assertEquals(new TriggerKey(identity), trigger.getValue().getKey());
        assertEquals(Date.from(scheduledDateTime.toInstant()), trigger.getValue().getStartTime());
        assertEquals(new JobKey(identity), trigger.getValue().getJobKey());
        assertEquals(Event.EXAMPLE.toString(), trigger.getValue().getJobDataMap().getString("event"));
        assertEquals(jurisdiction, trigger.getValue().getJobDataMap().get("jurisdiction"));
        assertEquals(caseType, trigger.getValue().getJobDataMap().getString("caseType"));
        assertEquals(caseId, trigger.getValue().getJobDataMap().getLong("caseId"));
    }

    @Test
    public void should_re_throw_custom_exception_when_scheduler_throws_it() throws SchedulerException {

        when(identityProvider.identity()).thenReturn(identity);
        when(scheduler.scheduleJob(any(JobDetail.class), any(Trigger.class))).thenThrow(SchedulerException.class);

        TimedEvent timedEvent = new TimedEvent(
            "",
            Event.EXAMPLE,
            ZonedDateTime.now(),
            jurisdiction,
            caseType,
            caseId
        );

        SchedulerProcessingException ex = assertThrows(
            SchedulerProcessingException.class,
            () -> schedulerService.schedule(timedEvent)
        );
        assertEquals(SchedulerException.class, ex.getCause().getClass());
        verify(identityProvider).identity();
        verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }
}
