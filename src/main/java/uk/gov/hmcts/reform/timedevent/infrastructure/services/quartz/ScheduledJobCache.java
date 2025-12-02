package uk.gov.hmcts.reform.timedevent.infrastructure.services.quartz;

import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@Slf4j
public class ScheduledJobCache {
    private final Scheduler quartzScheduler;
    private final ExpiringCache<String, List<String>> jobGroupNamesCache;
    private final ExpiringCache<String, Set<JobKey>> jobKeysCache;
    private final ExpiringCache<String, JobDetail> jobDetailCache;
    private final ExpiringCache<String, List<Trigger>> triggersOfJobCache;

    public ScheduledJobCache(Scheduler quartzScheduler) {
        this.quartzScheduler = quartzScheduler;

        this.jobGroupNamesCache = new ExpiringCache<>();
        this.jobKeysCache = new ExpiringCache<>();
        this.jobDetailCache = new ExpiringCache<>();
        this.triggersOfJobCache = new ExpiringCache<>();
    }

    public List<String> getJobGroupNames() throws SchedulerException {
        log.info("Getting job group names");

        Optional<List<String>> jobGroupNamesOpt = jobGroupNamesCache.get("JobGroupNames");
        if (jobGroupNamesOpt.isPresent()) {
            return jobGroupNamesOpt.get();
        } else {
            List<String> jobGroupNames = quartzScheduler.getJobGroupNames();
            jobGroupNamesCache.put("JobGroupNames", jobGroupNames, 3600000);
            return jobGroupNames;
        }
    }

    public Set<JobKey> getJobKeys(String groupName) throws SchedulerException {
        log.info("Getting job keys for group {}", groupName);

        Optional<Set<JobKey>> jobKeysOpt = jobKeysCache.get(groupName);
        if (jobKeysOpt.isPresent()) {
            return jobKeysOpt.get();
        } else {
            Set<JobKey> jobKeys = quartzScheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName));
            jobKeysCache.put(groupName, jobKeys, 300000);
            return jobKeys;
        }
    }

    public JobDetail getJobDetail(String groupName, JobKey jobKey) throws SchedulerException {
        log.info("Getting job detail for group {}, jobKey {}", groupName, jobKey.getName());

        Optional<JobDetail> jobDetailOpt = jobDetailCache.get(groupName + " " + jobKey.getName());
        if (jobDetailOpt.isPresent()) {
            return jobDetailOpt.get();
        } else {
            JobDetail jobDetail = quartzScheduler.getJobDetail(jobKey);
            jobDetailCache.put(groupName + " " + jobKey.getName(), jobDetail, 300000);
            return jobDetail;
        }
    }

    public List<Trigger> getTriggersOfJob(String groupName, JobKey jobKey) throws SchedulerException {
        log.info("Getting triggers for group {}, jobKey {}", jobKey.getGroup(), jobKey.getName());

        Optional<List<Trigger>> triggersOfJobOpt = triggersOfJobCache.get(groupName + " " + jobKey.getName());
        if (triggersOfJobOpt.isPresent()) {
            return triggersOfJobOpt.get();
        } else {
            List<Trigger> triggersOfJob = (List<Trigger>)quartzScheduler.getTriggersOfJob(jobKey);
            triggersOfJobCache.put(groupName + " " + jobKey.getName(), triggersOfJob, 300000);
            return triggersOfJob;
        }
    }
}
