package uk.gov.hmcts.reform.timedevent.infrastructure.services.quartz;

import java.time.ZonedDateTime;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.listeners.JobListenerSupport;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.entities.TimedEvent;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.services.SchedulerService;
import uk.gov.hmcts.reform.timedevent.infrastructure.services.DateTimeProvider;
import uk.gov.hmcts.reform.timedevent.infrastructure.services.exceptions.RetryableException;

@Slf4j
public class RetryJobListener extends JobListenerSupport {

    private final long durationInSeconds;
    private final long maxRetryNumber;
    private final SchedulerService schedulerService;
    private final DateTimeProvider dateTimeProvider;

    public RetryJobListener(
        long durationInSeconds,
        long maxRetryNumber,
        SchedulerService schedulerService,
        DateTimeProvider dateTimeProvider
    ) {
        this.durationInSeconds = durationInSeconds;
        this.maxRetryNumber = maxRetryNumber;
        this.schedulerService = schedulerService;
        this.dateTimeProvider = dateTimeProvider;
    }

    @Override
    public String getName() {
        return "RetryPolicyJobListener";
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        String identity = context.getJobDetail().getKey().getName();

        JobDataMap data = context.getJobDetail().getJobDataMap();
        String event = data.getString("event");
        long caseId = data.getLong("caseId");

        if (jobException instanceof RetryableException) {
            log.warn("A retryable exception was issued while executing a job so a retry will be scheduled. "
                    + "Stack trace of exception follows.",
                jobException);

            long retryCount = data.getLong("retryCount");

            if (retryCount <= maxRetryNumber) {
                ZonedDateTime newDate = calculateNextScheduledDate();
                String retriedIdentity = scheduleRetry(data, newDate, identity, retryCount + 1);

                log.info(
                    "Retry has been scheduled with new identity: {}, for event: {}, caseId: {}, date: {}. "
                        + "Retry #{} of {}",
                    retriedIdentity,
                    event,
                    caseId,
                    newDate.toString(),
                    retryCount + 1,
                    maxRetryNumber
                );
            } else {

                log.error(
                    "Max number of retries have been processed with the last identity: {}, for event: {}, caseId: {}",
                    identity,
                    event,
                    caseId
                );
            }

        }

        log.info("Job finished execution with identity: {}, for event: {}, caseId: {}", identity, event, caseId);
    }

    private String scheduleRetry(JobDataMap data, ZonedDateTime newDate, String identity, long retryCount) {

        TimedEvent timedEvent = new TimedEvent(
            identity,
            Event.fromString(data.getString("event")),
            newDate,
            data.getString("jurisdiction"),
            data.getString("caseType"),
            data.getLong("caseId")
        );

        log.info(
            "Automated scheduling of a retry for event: {}, case id: {} at: {}",
            timedEvent.getEvent().toString(),
            timedEvent.getCaseId(),
            timedEvent.getScheduledDateTime().toString()
        );

        return schedulerService.reschedule(timedEvent, retryCount);
    }

    private ZonedDateTime calculateNextScheduledDate() {
        return dateTimeProvider.now().plusSeconds(durationInSeconds);
    }
}
