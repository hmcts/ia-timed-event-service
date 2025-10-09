package uk.gov.hmcts.reform.timedevent.infrastructure.services.quartz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import uk.gov.hmcts.reform.timedevent.domain.entities.TimedEvent;
import uk.gov.hmcts.reform.timedevent.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.timedevent.infrastructure.services.exceptions.SchedulerProcessingException;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.timedevent.domain.entities.ccd.Event.SAVE_NOTIFICATIONS_TO_DATA;

@ExtendWith(MockitoExtension.class)
class ExistingScheduledJobFinderTest {

    @Mock
    private Scheduler quartzScheduler;

    private ExistingScheduledJobFinder jobFinder;

    private final String jurisdiction = "IA";
    private final String caseType = "Asylum";
    private final ZonedDateTime scheduledDateTime = ZonedDateTime.now();
    private final long caseId = 12345;

    @BeforeEach
    void setUp() {
        jobFinder = new ExistingScheduledJobFinder(quartzScheduler);
    }

    @Test
    void shouldReturnTimedEventId_whenMatchingJobExists() throws Exception {
        // given
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("event", SAVE_NOTIFICATIONS_TO_DATA.toString());
        jobDataMap.put("caseId", caseId);
        jobDataMap.put("scheduledDateTime", ZonedDateTime.now(ZoneId.systemDefault()));

        JobDetail jobDetail = mock(JobDetail.class);
        when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);

        String groupName = "testGroup";
        JobKey jobKey = new JobKey("job1", groupName);
        when(quartzScheduler.getJobGroupNames()).thenReturn(List.of(groupName));
        when(quartzScheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))).thenReturn(Set.of(jobKey));
        when(quartzScheduler.getJobDetail(jobKey)).thenReturn(jobDetail);

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
        assertThat(result.get()).isEqualTo("job1");
    }

    @Test
    void shouldNotReturnTimedEventId_whenMatchingJobExistsWithPreviousDay() throws Exception {
        // given
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("event", SAVE_NOTIFICATIONS_TO_DATA.toString());
        jobDataMap.put("caseId", caseId);
        jobDataMap.put("scheduledDateTime", ZonedDateTime.now(ZoneId.systemDefault()).minusDays(1));

        JobDetail jobDetail = mock(JobDetail.class);
        when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);

        String groupName = "testGroup";
        JobKey jobKey = new JobKey("job1", groupName);
        when(quartzScheduler.getJobGroupNames()).thenReturn(List.of(groupName));
        when(quartzScheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))).thenReturn(Set.of(jobKey));
        when(quartzScheduler.getJobDetail(jobKey)).thenReturn(jobDetail);
        when(quartzScheduler.getTriggersOfJob(jobKey)).thenReturn(emptyList());

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
    void shouldNotReturnTimedEventId_whenMatchingJobWithDateDoesNotExist() throws Exception {
        // given
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("event", SAVE_NOTIFICATIONS_TO_DATA.toString());
        jobDataMap.put("caseId", caseId);
        jobDataMap.put("scheduledDateTime", null);

        JobDetail jobDetail = mock(JobDetail.class);
        when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);

        String groupName = "testGroup";
        JobKey jobKey = new JobKey("job1", groupName);
        when(quartzScheduler.getJobGroupNames()).thenReturn(List.of(groupName));
        when(quartzScheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))).thenReturn(Set.of(jobKey));
        when(quartzScheduler.getJobDetail(jobKey)).thenReturn(jobDetail);

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

        when(quartzScheduler.getJobGroupNames()).thenReturn(List.of("group1"));
        when(quartzScheduler.getJobKeys(any())).thenReturn(Collections.emptySet());

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
        verifyNoInteractions(quartzScheduler);
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

        when(quartzScheduler.getJobGroupNames()).thenThrow(new SchedulerException("boom"));

        assertThatThrownBy(() -> jobFinder.getExistingSaveNotificationsToDataScheduledJob(timedEvent))
                .isInstanceOf(SchedulerProcessingException.class)
                .hasCauseInstanceOf(SchedulerException.class);
    }
}
