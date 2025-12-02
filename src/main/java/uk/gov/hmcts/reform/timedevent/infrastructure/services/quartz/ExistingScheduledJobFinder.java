package uk.gov.hmcts.reform.timedevent.infrastructure.services.quartz;

import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.timedevent.domain.entities.TimedEvent;
import uk.gov.hmcts.reform.timedevent.infrastructure.services.exceptions.SchedulerProcessingException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static uk.gov.hmcts.reform.timedevent.domain.entities.ccd.Event.SAVE_NOTIFICATIONS_TO_DATA;

@Component
@Slf4j
public class ExistingScheduledJobFinder {
    private final ScheduledJobCache scheduledJobCache;

    public ExistingScheduledJobFinder(ScheduledJobCache scheduledJobCache) {
        this.scheduledJobCache = scheduledJobCache;
    }

    public Optional<String> getExistingSaveNotificationsToDataScheduledJob(TimedEvent timedEvent) {
        log.info("---------Getting existing scheduled job for {}", timedEvent.getEvent().toString());

        if (timedEvent.getEvent().toString().equals(SAVE_NOTIFICATIONS_TO_DATA.toString())) {
            log.info("---------222Getting existing scheduled job for {}", timedEvent.getEvent());

            try {
                List<String> jobGroupNames = scheduledJobCache.getJobGroupNames();
                for (String groupName : jobGroupNames) {
                    log.info("-------------------Found scheduled job for group {}", groupName);

                    Set<JobKey> jobKeys = scheduledJobCache.getJobKeys(groupName);
                    for (JobKey jobKey : jobKeys) {
                        log.info("---------Found jobKey {}", jobKey.getName());

                        JobDetail jobDetail = scheduledJobCache.getJobDetail(groupName, jobKey);
                        JobDataMap jobDataMap = jobDetail.getJobDataMap();
                        List<? extends Trigger> jobTriggers = scheduledJobCache.getTriggersOfJob(groupName, jobKey);
                        log.info("---------Found jobTriggers {}", jobTriggers.size());
                        log.info("---------String.valueOf(jobDataMap.get(\"event\")) {}", String.valueOf(jobDataMap.get("event")));
                        log.info("---------String.valueOf(jobDataMap.get(\"caseId\")) {}", String.valueOf(jobDataMap.get("caseId")));

                        if (String.valueOf(jobDataMap.get("event")).equals(SAVE_NOTIFICATIONS_TO_DATA.toString())
                                && String.valueOf(jobDataMap.get("caseId")).equals(String.valueOf(timedEvent.getCaseId()))
                                && !jobTriggers.isEmpty()) {
                            return Optional.of(jobKey.getName());
                        }
                    }
                }
            } catch (SchedulerException e) {
                throw new SchedulerProcessingException(e);
            }
        }

        return Optional.empty();
    }
}
