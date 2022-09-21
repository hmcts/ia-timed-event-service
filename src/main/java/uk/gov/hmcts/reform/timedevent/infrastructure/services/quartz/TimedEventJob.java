package uk.gov.hmcts.reform.timedevent.infrastructure.services.quartz;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.entities.EventExecution;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.services.EventExecutor;
import uk.gov.hmcts.reform.timedevent.infrastructure.services.RetryableExceptionHandler;

@Slf4j
@Component
public class TimedEventJob implements Job {

    private final EventExecutor eventExecutor;
    private final RetryableExceptionHandler exceptionHandler;

    public TimedEventJob(EventExecutor eventExecutor, RetryableExceptionHandler exceptionHandler) {
        this.eventExecutor = eventExecutor;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap data = context.getJobDetail().getJobDataMap();

        try {
            eventExecutor.execute(
                new EventExecution(
                    Event.fromString(data.getString("event")),
                    data.getString("jurisdiction"),
                    data.getString("caseType"),
                    data.getLong("caseId")
                )
            );
        } catch (Exception e) {
            log.error(e.getMessage(), e);

            exceptionHandler.wrapException(e);
        }
    }
}
