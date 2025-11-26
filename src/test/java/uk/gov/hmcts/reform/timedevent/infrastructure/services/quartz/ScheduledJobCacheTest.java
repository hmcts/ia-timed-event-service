package uk.gov.hmcts.reform.timedevent.infrastructure.services.quartz;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledJobCacheTest {

    private Scheduler scheduler;
    private TestScheduledJobCache cache;
    private CacheManager cacheManager;

    @BeforeEach
    void setup() {
        scheduler = mock(Scheduler.class);

        // Create caffeine caches for test
        CaffeineCache groupNamesCache = new CaffeineCache("scheduledJobGroupNames",
                Caffeine.newBuilder().build());
        CaffeineCache jobKeysCache = new CaffeineCache("scheduledJobKeys",
                Caffeine.newBuilder().build());
        CaffeineCache jobDetailCache = new CaffeineCache("scheduledJobDetail",
                Caffeine.newBuilder().build());
        CaffeineCache triggersCache = new CaffeineCache("scheduledJobTriggers",
                Caffeine.newBuilder().build());

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(groupNamesCache, jobKeysCache, jobDetailCache, triggersCache));
        manager.initializeCaches();

        this.cacheManager = manager;
        this.cache = new TestScheduledJobCache(scheduler, cacheManager);
    }

    @Test
    void shouldReturnJobGroupNames() throws Exception {
        List<String> mockGroups = List.of("group1", "group2");

        when(scheduler.getJobGroupNames()).thenReturn(mockGroups);

        List<String> result1 = cache.getJobGroupNames();

        assertThat(result1).isEqualTo(mockGroups);

        verify(scheduler, times(1)).getJobGroupNames();
    }

    @Test
    void shouldReturnJobKeys() throws Exception {
        JobKey key = new JobKey("job1", "group1");
        Set<JobKey> mockKeys = Set.of(key);

        when(scheduler.getJobKeys(GroupMatcher.jobGroupEquals("group1")))
                .thenReturn(mockKeys);

        Set<JobKey> result1 = cache.getJobKeys("group1");

        assertThat(result1).containsExactly(key);

        verify(scheduler, times(1))
                .getJobKeys(GroupMatcher.jobGroupEquals("group1"));
    }

    @Test
    void shouldReturnJobDetail() throws Exception {
        JobKey key = new JobKey("jobA");
        JobDetail detail = Mockito.mock(JobDetail.class);

        when(scheduler.getJobDetail(key)).thenReturn(detail);

        JobDetail result1 = cache.getJobDetail(key);

        assertThat(result1).isEqualTo(detail);

        verify(scheduler, times(1)).getJobDetail(key);
    }

    @Test
    void shouldReturnTriggers() throws Exception {
        JobKey key = new JobKey("jobA");
        Trigger trigger = mock(Trigger.class);
        List<Trigger> triggers = List.of(trigger);

        when(scheduler.getTriggersOfJob(key)).thenReturn((List)triggers);

        List<Trigger> result1 = (List<Trigger>) cache.getTriggersOfJob(key);

        assertThat(result1).containsExactly(trigger);

        verify(scheduler, times(1)).getTriggersOfJob(key);
    }

    class TestScheduledJobCache {
        private final Scheduler quartzScheduler;
        private final CacheManager cacheManager;

        public TestScheduledJobCache(Scheduler quartzScheduler, CacheManager cacheManager) {
            this.quartzScheduler = quartzScheduler;
            this.cacheManager = cacheManager;
        }

        public List<String> getJobGroupNames() throws SchedulerException {
            return quartzScheduler.getJobGroupNames();
        }

        public Set<JobKey> getJobKeys(String groupName) throws SchedulerException {
            return quartzScheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName));
        }

        public JobDetail getJobDetail(JobKey jobKey) throws SchedulerException {
            return quartzScheduler.getJobDetail(jobKey);
        }

        public List<? extends Trigger> getTriggersOfJob(JobKey jobKey) throws SchedulerException {
            return quartzScheduler.getTriggersOfJob(jobKey);
        }
    }
}
