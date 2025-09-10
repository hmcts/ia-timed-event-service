package uk.gov.hmcts.reform.timedevent.infrastructure.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.entities.ccd.Event;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class EventExecution {

    private Event event;
    private String jurisdiction;
    private String caseType;
    private long caseId;

}
