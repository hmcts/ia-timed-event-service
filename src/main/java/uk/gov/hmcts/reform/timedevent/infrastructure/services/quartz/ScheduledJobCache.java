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
            log.info("-----Got {} job group names from cache", jobGroupNamesOpt.get().size());
            return jobGroupNamesOpt.get();
        } else {
            List<String> jobGroupNames = quartzScheduler.getJobGroupNames();
            jobGroupNamesCache.put("JobGroupNames", jobGroupNames, 3600000);
            log.info("-----Put {} job group names to cache", jobGroupNames.size());
            return jobGroupNames;
        }
    }

    public Set<JobKey> getJobKeys(String groupName) throws SchedulerException {
        log.info("Getting job keys for group {}", groupName);

        Optional<Set<JobKey>> jobKeysOpt = jobKeysCache.get(groupName);
        if (jobKeysOpt.isPresent()) {
            log.info("-----Got {} job keys from cache", jobKeysOpt.get().size());
            return jobKeysOpt.get();
        } else {
            Set<JobKey> jobKeys = quartzScheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName));
            log.info("-----Put {} job keys to cache", jobKeys.size());
            jobKeysCache.put(groupName, jobKeys, 300000);
            return jobKeys;
        }
    }

    public JobDetail getJobDetail(String groupName, JobKey jobKey) throws SchedulerException {
        log.info("Getting job detail for group {}, jobKey {}", groupName, jobKey.getName());

        Optional<JobDetail> jobDetailOpt = jobDetailCache.get(groupName + " " + jobKey.getName());
        if (jobDetailOpt.isPresent()) {
            log.info("-----Got job detail from cache {}", jobKey.getName());
            return jobDetailOpt.get();
        } else {
            JobDetail jobDetail = quartzScheduler.getJobDetail(jobKey);
            log.info("-----Put job detail to cache {}", jobKey.getName());
            jobDetailCache.put(groupName + " " + jobKey.getName(), jobDetail, 300000);
            return jobDetail;
        }
    }

    public List<Trigger> getTriggersOfJob(String groupName, JobKey jobKey) throws SchedulerException {
        log.info("Getting triggers for group {}, jobKey {}", jobKey.getGroup(), jobKey.getName());

        Optional<List<Trigger>> triggersOfJobOpt = triggersOfJobCache.get(groupName + " " + jobKey.getName());
        if (triggersOfJobOpt.isPresent()) {
            log.info("-----Got triggers of job from cache {}: {}", groupName + " " + jobKey.getName(), triggersOfJobOpt.get().size());
            return triggersOfJobOpt.get();
        } else {
            List<Trigger> triggersOfJob = (List<Trigger>)quartzScheduler.getTriggersOfJob(jobKey);

            if (!triggersOfJob.isEmpty()) {
                log.info("-----Put triggers of job to cache {}", triggersOfJob.size());
                triggersOfJobCache.put(groupName + " " + jobKey.getName(), triggersOfJob, 300000);
            }

            return triggersOfJob;
        }
    }
}
