package uk.gov.hmcts.reform.timedevent.domain.services;

import java.util.Optional;
import uk.gov.hmcts.reform.timedevent.domain.entities.TimedEvent;

public interface SchedulerService {

    String schedule(TimedEvent timedEvent);

    String reschedule(TimedEvent timedEvent);

    boolean deleteSchedule(String jobKey);

    Optional<TimedEvent> get(String identity);
}
