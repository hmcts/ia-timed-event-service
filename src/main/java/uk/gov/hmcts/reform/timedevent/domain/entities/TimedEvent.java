package uk.gov.hmcts.reform.timedevent.domain.entities;

import java.time.ZonedDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.timedevent.domain.entities.ccd.Event;

@AllArgsConstructor
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TimedEvent {

    private String id;
    private Event event;
    private ZonedDateTime scheduledDateTime;
    private String jurisdiction;
    private String caseType;
    private long caseId;

}
