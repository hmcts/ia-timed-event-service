package uk.gov.hmcts.reform.timedevent.infrastructure.services.quartz;

import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.timedevent.domain.entities.TimedEvent;
import uk.gov.hmcts.reform.timedevent.infrastructure.services.exceptions.SchedulerProcessingException;

import java.util.Optional;

import static uk.gov.hmcts.reform.timedevent.domain.entities.ccd.Event.SAVE_NOTIFICATIONS_TO_DATA;

@Component
@Slf4j
public class ExistingScheduledJobFinder {
    private final Scheduler quartzScheduler;

    public ExistingScheduledJobFinder(Scheduler quartzScheduler) {
        this.quartzScheduler = quartzScheduler;
    }

    public Optional<String> getExistingSaveNotificationsToDataScheduledJob(TimedEvent timedEvent) {
        if (timedEvent.getEvent().toString().equals(SAVE_NOTIFICATIONS_TO_DATA.toString())) {
            try {
                for (String groupName : quartzScheduler.getJobGroupNames()) {
                    for (JobKey jobKey : quartzScheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                        JobDetail jobDetail = quartzScheduler.getJobDetail(jobKey);
                        JobDataMap jobDataMap = jobDetail.getJobDataMap();
                        String event = jobDataMap.get("event").toString();
                        String caseId = jobDataMap.get("caseId").toString();
                        if (event.equals(SAVE_NOTIFICATIONS_TO_DATA.toString())
                                && caseId.equals(String.valueOf(timedEvent.getCaseId()))) {
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
