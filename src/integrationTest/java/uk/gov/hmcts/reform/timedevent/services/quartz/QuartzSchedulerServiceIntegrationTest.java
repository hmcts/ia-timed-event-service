package uk.gov.hmcts.reform.timedevent.services.quartz;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.entities.TimedEvent;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.services.SchedulerService;
import uk.gov.hmcts.reform.timedevent.infrastructure.services.quartz.QuartzSchedulerService;
import uk.gov.hmcts.reform.timedevent.testutils.SpringBootIntegrationTest;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QuartzSchedulerServiceIntegrationTest extends SpringBootIntegrationTest {

    @Autowired
    SchedulerService schedulerService;

    @Test
    void should_delete_scheduled_tasks() {
        // Given: a scheduled task

        TimedEvent timedEvent = new TimedEvent(
            "some id",
            Event.EXAMPLE,
            ZonedDateTime.now().plus(1, DAYS),
            "IA",
            "Asylum",
            12345
        );

        String newIdentity = schedulerService.schedule(timedEvent);

        // When: I try to delete a scheduled task with the same identity
        boolean result = schedulerService.deleteSchedule(newIdentity);

        // Then: it should respond true, indicating the task has been deleted
        assertTrue(result);
    }


}
