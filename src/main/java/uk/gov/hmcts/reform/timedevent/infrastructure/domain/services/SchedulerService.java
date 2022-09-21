package uk.gov.hmcts.reform.timedevent.infrastructure.domain.services;

import java.util.Optional;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.entities.TimedEvent;

public interface SchedulerService {

    String schedule(TimedEvent timedEvent);

    String reschedule(TimedEvent timedEvent, long retryCount);

    Optional<TimedEvent> get(String identity);
}
