package uk.gov.hmcts.reform.timedevent.infrastructure.services.quartz;

import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class ScheduledJobCache {
    private final Scheduler quartzScheduler;

    public ScheduledJobCache(Scheduler quartzScheduler) {
        this.quartzScheduler = quartzScheduler;
    }

    @Cacheable(value = "scheduledJobGroupNames")
    public List<String> getJobGroupNames() throws SchedulerException {
        log.info("Getting job group names");

        return quartzScheduler.getJobGroupNames();
    }

    @Cacheable(value = "scheduledJobKeys")
    public Set<JobKey> getJobKeys(String groupName) throws SchedulerException {
        log.info("Getting job keys for {}", groupName);

        return quartzScheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName));
    }

    @Cacheable(value = "scheduledJobDetail")
    public JobDetail getJobDetail(JobKey jobKey) throws SchedulerException {
        log.info("Getting job detail for {}", jobKey);

        return quartzScheduler.getJobDetail(jobKey);
    }

    @Cacheable(value = "scheduledJobTriggers")
    public List<? extends Trigger> getTriggersOfJob(JobKey jobKey) throws SchedulerException {
        log.info("Getting triggers for {}", jobKey);

        return quartzScheduler.getTriggersOfJob(jobKey);
    }
}
