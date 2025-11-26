package uk.gov.hmcts.reform.timedevent.infrastructure.services.quartz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.*;
import uk.gov.hmcts.reform.timedevent.domain.entities.TimedEvent;
import uk.gov.hmcts.reform.timedevent.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.timedevent.infrastructure.services.exceptions.SchedulerProcessingException;

import java.time.ZonedDateTime;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.timedevent.domain.entities.ccd.Event.SAVE_NOTIFICATIONS_TO_DATA;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class ExistingScheduledJobFinderTest {

    @Mock
    private ScheduledJobCache scheduledJobCache;

    private ExistingScheduledJobFinder jobFinder;

    private final String jurisdiction = "IA";
    private final String caseType = "Asylum";
    private final ZonedDateTime scheduledDateTime = ZonedDateTime.now();
    private final long caseId = 12345;

    @BeforeEach
    void setUp() {
        jobFinder = new ExistingScheduledJobFinder(scheduledJobCache);
    }

    @Test
    void shouldReturnTimedEventId_whenMatchingJobExistsAndIsScheduled() throws Exception {
        // given
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("event", SAVE_NOTIFICATIONS_TO_DATA.toString());
        jobDataMap.put("caseId", caseId);

        JobDetail jobDetail = mock(JobDetail.class);
        when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);

        String groupName = "testGroup";
        JobKey jobKey = new JobKey("job1", groupName);
        when(scheduledJobCache.getJobGroupNames()).thenReturn(List.of(groupName));
        when(scheduledJobCache.getJobKeys(groupName)).thenReturn(Set.of(jobKey));
        when(scheduledJobCache.getJobDetail(jobKey)).thenReturn(jobDetail);
        List<? extends Trigger> triggers = singletonList(mock(Trigger.class));
        when(scheduledJobCache.getTriggersOfJob(jobKey)).thenReturn((List)triggers);

        // when
        TimedEvent timedEvent = new TimedEvent(
                "",
                SAVE_NOTIFICATIONS_TO_DATA,
                scheduledDateTime,
                jurisdiction,
                caseType,
                caseId
        );
        Optional<String> result = jobFinder.getExistingSaveNotificationsToDataScheduledJob(timedEvent);

        // then
        assertThat(result).isPresent();
        assertThat("job1").isEqualTo(result.get());
    }

    @Test
    void shouldReturnTimedEventId_whenMatchingJobExistsAndIsNotScheduled() throws Exception {
        // given
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("event", SAVE_NOTIFICATIONS_TO_DATA.toString());
        jobDataMap.put("caseId", caseId);

        JobDetail jobDetail = mock(JobDetail.class);
        when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);

        String groupName = "testGroup";
        JobKey jobKey = new JobKey("job1", groupName);
        when(scheduledJobCache.getJobGroupNames()).thenReturn(List.of(groupName));
        when(scheduledJobCache.getJobKeys(groupName)).thenReturn(Set.of(jobKey));
        when(scheduledJobCache.getJobDetail(jobKey)).thenReturn(jobDetail);
        when(scheduledJobCache.getTriggersOfJob(jobKey)).thenReturn(emptyList());

        // when
        TimedEvent timedEvent = new TimedEvent(
                "",
                SAVE_NOTIFICATIONS_TO_DATA,
                scheduledDateTime,
                jurisdiction,
                caseType,
                caseId
        );
        Optional<String> result = jobFinder.getExistingSaveNotificationsToDataScheduledJob(timedEvent);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmpty_whenNoMatchingJob() throws Exception {
        TimedEvent timedEvent = new TimedEvent(
                "",
                SAVE_NOTIFICATIONS_TO_DATA,
                scheduledDateTime,
                jurisdiction,
                caseType,
                caseId
        );

        when(scheduledJobCache.getJobGroupNames()).thenReturn(List.of("group1"));
        when(scheduledJobCache.getJobKeys(any())).thenReturn(Collections.emptySet());

        Optional<String> result = jobFinder.getExistingSaveNotificationsToDataScheduledJob(timedEvent);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmpty_whenEventIsNotSaveNotificationsToData() {
        TimedEvent timedEvent = new TimedEvent(
                "",
                Event.EXAMPLE,
                scheduledDateTime,
                jurisdiction,
                caseType,
                caseId
        );

        Optional<String> result = jobFinder.getExistingSaveNotificationsToDataScheduledJob(timedEvent);

        assertThat(result).isEmpty();
        verifyNoInteractions(scheduledJobCache);
    }

    @Test
    void shouldThrowSchedulerProcessingException_whenSchedulerThrows() throws Exception {
        TimedEvent timedEvent = new TimedEvent(
                "",
                SAVE_NOTIFICATIONS_TO_DATA,
                scheduledDateTime,
                jurisdiction,
                caseType,
                caseId
        );

        when(scheduledJobCache.getJobGroupNames()).thenThrow(new SchedulerException("boom"));

        assertThatThrownBy(() -> jobFinder.getExistingSaveNotificationsToDataScheduledJob(timedEvent))
                .isInstanceOf(SchedulerProcessingException.class)
                .hasCauseInstanceOf(SchedulerException.class);
    }
}
