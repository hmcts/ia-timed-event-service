package uk.gov.hmcts.reform.timedevent.infrastructure.services.quartz;

import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.timedevent.domain.entities.TimedEvent;
import uk.gov.hmcts.reform.timedevent.infrastructure.services.exceptions.SchedulerProcessingException;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.timedevent.domain.entities.ccd.Event.SAVE_NOTIFICATIONS_TO_DATA;

@Component
@Slf4j
public class ExistingScheduledJobFinder {
    private final ScheduledJobCache scheduledJobCache;

    public ExistingScheduledJobFinder(ScheduledJobCache scheduledJobCache) {
        this.scheduledJobCache = scheduledJobCache;
    }

    public Optional<String> getExistingSaveNotificationsToDataScheduledJob(TimedEvent timedEvent) {
        if (timedEvent.getEvent().toString().equals(SAVE_NOTIFICATIONS_TO_DATA.toString())) {
            try {
                for (String groupName : scheduledJobCache.getJobGroupNames()) {
                    for (JobKey jobKey : scheduledJobCache.getJobKeys(groupName)) {
                        JobDetail jobDetail = scheduledJobCache.getJobDetail(jobKey);
                        JobDataMap jobDataMap = jobDetail.getJobDataMap();
                        List<? extends Trigger> jobTriggers = scheduledJobCache.getTriggersOfJob(jobKey);

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
