package uk.gov.hmcts.reform.timedevent.infrastructure.services.quartz;

import static java.time.ZoneOffset.UTC;

import com.google.common.collect.ImmutableMap;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Optional;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.quartz.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.timedevent.domain.entities.TimedEvent;
import uk.gov.hmcts.reform.timedevent.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.timedevent.domain.services.SchedulerService;
import uk.gov.hmcts.reform.timedevent.infrastructure.services.IdentityProvider;
import uk.gov.hmcts.reform.timedevent.infrastructure.services.exceptions.SchedulerProcessingException;

@Slf4j
@Service
public class QuartzSchedulerService implements SchedulerService {

    private final Scheduler quartzScheduler;
    private final IdentityProvider identityProvider;
    private final ExistingScheduledJobFinder existingScheduledJobFinder;

    public QuartzSchedulerService(
        Scheduler quartzScheduler,
        IdentityProvider identityProvider,
        ExistingScheduledJobFinder existingScheduledJobFinder
    ) {
        this.quartzScheduler = quartzScheduler;
        this.identityProvider = identityProvider;
        this.existingScheduledJobFinder = existingScheduledJobFinder;
    }

    @Override
    @Transactional
    public String schedule(TimedEvent timedEvent) {
        Optional<String> existingScheduledJobOpt =
                existingScheduledJobFinder.getExistingSaveNotificationsToDataScheduledJob(timedEvent);

        if (existingScheduledJobOpt.isPresent()) {
            log.info(
                    "Timed Event already scheduled for event: {}, case id: {}, timed event id: {}, at: {}",
                    timedEvent.getEvent().toString(),
                    timedEvent.getCaseId(),
                    existingScheduledJobOpt.get(),
                    timedEvent.getScheduledDateTime().toString()
            );
            return existingScheduledJobOpt.get();
        } else {
            String identity = identityProvider.identity();

            Pair<JobDetail, Trigger> jobAndTrigger = createJobAndTrigger(
                    new TimedEvent(
                            identity,
                            timedEvent.getEvent(),
                            timedEvent.getScheduledDateTime(),
                            timedEvent.getJurisdiction(),
                            timedEvent.getCaseType(),
                            timedEvent.getCaseId()
                    )
            );

            try {
                quartzScheduler.scheduleJob(jobAndTrigger.getLeft(), jobAndTrigger.getRight());

                String timedEventId = jobAndTrigger.getRight().getKey().getName();

                log.info(
                        "Timed Event scheduled for event: {}, case id: {}, timed event id: {}, at: {}",
                        timedEvent.getEvent().toString(),
                        timedEvent.getCaseId(),
                        timedEventId,
                        timedEvent.getScheduledDateTime().toString()
                );

                return timedEventId;

            } catch (SchedulerException e) {

                throw new SchedulerProcessingException(e);
            }
        }
    }

    @Override
    @Transactional
    public String reschedule(TimedEvent timedEvent) {

        Pair<JobDetail, Trigger> jobAndTrigger = createJobAndTrigger(timedEvent);

        try {

            Date newSchedule = quartzScheduler.rescheduleJob(new TriggerKey(timedEvent.getId()), jobAndTrigger.getRight());

            if (newSchedule != null) {
                log.info(
                    "Timed Event re-scheduled for event: {}, case id: {} at: {}",
                    timedEvent.getEvent().toString(),
                    timedEvent.getCaseId(),
                    timedEvent.getScheduledDateTime().toString()
                );
            } else {
                // 2023-08-10 it's an error condition and will cause problems...
                // ... but we continue execution for compatibility while we try to understand what's happening
                log.error(
                    "Timed Event re-scheduling failed for event: {}, case id: {} at: {}, timedEvent id: {}",
                    timedEvent.getEvent().toString(),
                    timedEvent.getCaseId(),
                    timedEvent.getScheduledDateTime().toString(),
                    timedEvent.getId()
                );
            }

            return jobAndTrigger.getRight().getKey().getName();

        } catch (SchedulerException e) {

            throw new SchedulerProcessingException(e);
        }
    }

    @SneakyThrows
    @Transactional
    public boolean deleteSchedule(String jobKey) {
        TimedEvent timedEvent = get(jobKey).orElseThrow();
        Pair<JobDetail, Trigger> jobAndTrigger = createJobAndTrigger(timedEvent);
        return quartzScheduler.deleteJob(jobAndTrigger.getLeft().getKey());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TimedEvent> get(String identity) {

        try {

            return Optional.ofNullable(quartzScheduler.getTrigger(new TriggerKey(identity)))
                .map(trigger -> {
                    JobDataMap data = trigger.getJobDataMap();

                    return new TimedEvent(
                        identity,
                        Event.fromString(data.getString("event")),
                        ZonedDateTime.ofInstant(trigger.getFinalFireTime().toInstant(), UTC),
                        data.getString("jurisdiction"),
                        data.getString("caseType"),
                        data.getLongFromString("caseId")
                    );
                });

        } catch (SchedulerException e) {

            throw new SchedulerProcessingException(e);
        }
    }

    private Pair<JobDetail, Trigger> createJobAndTrigger(TimedEvent timedEvent) {

        JobDataMap data = new JobDataMap(
            new ImmutableMap.Builder<String, String>()
                .put("jurisdiction", timedEvent.getJurisdiction())
                .put("caseType", timedEvent.getCaseType())
                .put("caseId", String.valueOf(timedEvent.getCaseId()))
                .put("event", timedEvent.getEvent().toString())
                .build()
        );

        JobDetail job = JobBuilder.newJob().ofType(TimedEventJob.class)
            .storeDurably()
            .withIdentity(timedEvent.getId())
            .withDescription("Timed Event job")
            .usingJobData(data)
            .build();

        Trigger trigger = TriggerBuilder.newTrigger().forJob(job)
            .withIdentity(timedEvent.getId())
            .withDescription("Timed Event trigger")
            .usingJobData(data)
            .startAt(Date.from(timedEvent.getScheduledDateTime().toInstant()))
            .build();

        return new ImmutablePair<>(job, trigger);
    }

}
