package uk.gov.hmcts.reform.timedevent.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.timedevent.domain.services.SchedulerService;
import uk.gov.hmcts.reform.timedevent.infrastructure.services.DateTimeProvider;
import uk.gov.hmcts.reform.timedevent.infrastructure.services.quartz.RetryJobListener;

@Slf4j
@Configuration
public class QuartzConfiguration {

    @Value("${retry.durationInSeconds}")
    private long retryDurationInSeconds;

    @Value("${retry.maxRetryNumber}")
    private long maxRetryNumber;

    @Bean
    public RetryJobListener retryJobListener(Scheduler quartzScheduler,
                                             SchedulerService schedulerService,
                                             DateTimeProvider dateTimeProvider) throws SchedulerException {

        RetryJobListener retryJobListener = new RetryJobListener(retryDurationInSeconds, maxRetryNumber, schedulerService, dateTimeProvider);

        quartzScheduler.getListenerManager().addJobListener(retryJobListener);

        log.info("Job Listener has been registered in Quartz Scheduler: " + retryJobListener.getName());

        return retryJobListener;
    }
}
