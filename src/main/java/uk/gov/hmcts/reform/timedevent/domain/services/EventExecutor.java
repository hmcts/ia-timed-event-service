package uk.gov.hmcts.reform.timedevent.domain.services;

import uk.gov.hmcts.reform.timedevent.domain.entities.EventExecution;

public interface EventExecutor {

    void execute(EventExecution eventExecution);
}
