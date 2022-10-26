package uk.gov.hmcts.reform.timedevent.infrastructure.domain.services;

import uk.gov.hmcts.reform.timedevent.infrastructure.domain.entities.EventExecution;

public interface EventExecutor {

    void execute(EventExecution eventExecution);
}
