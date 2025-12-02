package uk.gov.hmcts.reform.timedevent.infrastructure.services.quartz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;

import java.util.*;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScheduledJobCacheTest {

    private Scheduler scheduler;
    private ScheduledJobCache cache;

    @BeforeEach
    void setup() {
        scheduler = mock(Scheduler.class);
        cache = new ScheduledJobCache(scheduler);
    }

    @Test
    void shouldFetchJobGroupNamesOnFirstCall() throws Exception {
        List<String> groupNames = Arrays.asList("group1", "group2");

        when(scheduler.getJobGroupNames()).thenReturn(groupNames);

        List<String> result = cache.getJobGroupNames();

        assertEquals(groupNames, result);
        verify(scheduler, times(1)).getJobGroupNames();
    }

    @Test
    void shouldReturnJobGroupNamesFromCacheOnSecondCall() throws Exception {
        List<String> groupNames = Arrays.asList("group1", "group2");

        when(scheduler.getJobGroupNames()).thenReturn(groupNames);

        cache.getJobGroupNames();  // populates cache
        cache.getJobGroupNames();  // should come from cache

        verify(scheduler, times(1)).getJobGroupNames();
    }

    @Test
    void shouldFetchJobKeysOnFirstCall() throws Exception {
        Set<JobKey> keys = Set.of(new JobKey("job1", "group1"));
        when(scheduler.getJobKeys(GroupMatcher.jobGroupEquals("group1"))).thenReturn(keys);

        Set<JobKey> result = cache.getJobKeys("group1");

        assertEquals(keys, result);
        verify(scheduler, times(1))
                .getJobKeys(GroupMatcher.jobGroupEquals("group1"));
    }

    @Test
    void shouldReturnJobKeysFromCacheOnSecondCall() throws Exception {
        Set<JobKey> keys = Set.of(new JobKey("job1", "group1"));
        when(scheduler.getJobKeys(GroupMatcher.jobGroupEquals("group1"))).thenReturn(keys);

        cache.getJobKeys("group1");
        cache.getJobKeys("group1");

        verify(scheduler, times(1))
                .getJobKeys(GroupMatcher.jobGroupEquals("group1"));
    }

    @Test
    void shouldFetchJobDetailOnFirstCall() throws Exception {
        JobKey jobKey = new JobKey("jobA", "groupX");
        JobDetail detail = mock(JobDetail.class);

        when(scheduler.getJobDetail(jobKey)).thenReturn(detail);

        JobDetail result = cache.getJobDetail("groupX", jobKey);

        assertEquals(detail, result);
        verify(scheduler, times(1)).getJobDetail(jobKey);
    }

    @Test
    void shouldReturnJobDetailFromCacheOnSecondCall() throws Exception {
        JobKey jobKey = new JobKey("jobA", "groupX");
        JobDetail detail = mock(JobDetail.class);

        when(scheduler.getJobDetail(jobKey)).thenReturn(detail);

        cache.getJobDetail("groupX", jobKey);
        cache.getJobDetail("groupX", jobKey);

        verify(scheduler, times(1)).getJobDetail(jobKey);
    }

    @Test
    void shouldFetchTriggersOnFirstCall() throws Exception {
        JobKey jobKey = new JobKey("job1", "group1");
        Trigger trigger = mock(Trigger.class);
        List<Trigger> triggers = singletonList(trigger);

        when(scheduler.getTriggersOfJob(jobKey)).thenReturn((List) triggers);

        List<Trigger> result = cache.getTriggersOfJob("group1", jobKey);

        assertEquals(triggers, result);
        verify(scheduler, times(1)).getTriggersOfJob(jobKey);
    }

    @Test
    void shouldCacheTriggersWhenNotEmpty() throws Exception {
        JobKey jobKey = new JobKey("job1", "group1");
        Trigger trigger = mock(Trigger.class);
        List<Trigger> triggers = singletonList(trigger);

        when(scheduler.getTriggersOfJob(jobKey)).thenReturn((List) triggers);

        cache.getTriggersOfJob("group1", jobKey); // populates cache
        cache.getTriggersOfJob("group1", jobKey); // should hit cache

        verify(scheduler, times(1)).getTriggersOfJob(jobKey);
    }
}
