package uk.gov.hmcts.reform.timedevent.infrastructure.services.quartz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ScheduledJobCacheTest {

    @Mock
    private Scheduler scheduler;

    private ScheduledJobCache cache;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        cache = new ScheduledJobCache(scheduler);
    }

    @Test
    void shouldCacheJobGroupNames() throws Exception {
        List<String> groups = List.of("A", "B");
        when(scheduler.getJobGroupNames()).thenReturn(groups);

        List<String> r1 = cache.getJobGroupNames();
        List<String> r2 = cache.getJobGroupNames();

        assertThat(r1).containsExactly("A", "B");
        assertThat(r2).containsExactly("A", "B");

        verify(scheduler, times(1)).getJobGroupNames();
    }

    @Test
    void shouldCacheJobKeys() throws Exception {
        String group = "MyGroup";

        JobKey key1 = new JobKey("job1", group);
        JobKey key2 = new JobKey("job2", group);

        Set<JobKey> jobKeys = Set.of(key1, key2);

        when(scheduler.getJobKeys(GroupMatcher.jobGroupEquals(group))).thenReturn(jobKeys);

        Set<JobKey> r1 = cache.getJobKeys(group);
        Set<JobKey> r2 = cache.getJobKeys(group);

        assertThat(r1).containsExactlyInAnyOrder(key1, key2);
        assertThat(r2).containsExactlyInAnyOrder(key1, key2);

        verify(scheduler, times(1)).getJobKeys(GroupMatcher.jobGroupEquals(group));
    }

    @Test
    void shouldCacheJobDetail() throws Exception {
        String group = "G1";
        JobKey jobKey = new JobKey("jobA", group);

        JobDetail detail = mock(JobDetail.class);

        when(scheduler.getJobDetail(jobKey)).thenReturn(detail);

        JobDetail r1 = cache.getJobDetail(group, jobKey);
        JobDetail r2 = cache.getJobDetail(group, jobKey);

        assertThat(r1).isSameAs(detail);
        assertThat(r2).isSameAs(detail);

        verify(scheduler, times(1)).getJobDetail(jobKey);
    }

    @Test
    void shouldCacheTriggersOfJob() throws Exception {
        String group = "G1";
        JobKey jobKey = new JobKey("jobA", group);

        Trigger trigger = mock(Trigger.class);
        List<Trigger> triggers = List.of(trigger);

        when(scheduler.getTriggersOfJob(jobKey)).thenReturn((List) triggers);

        List<Trigger> r1 = cache.getTriggersOfJob(group, jobKey);
        List<Trigger> r2 = cache.getTriggersOfJob(group, jobKey);

        assertThat(r1).containsExactly(trigger);
        assertThat(r2).containsExactly(trigger);

        verify(scheduler, times(1)).getTriggersOfJob(jobKey);
    }
}
