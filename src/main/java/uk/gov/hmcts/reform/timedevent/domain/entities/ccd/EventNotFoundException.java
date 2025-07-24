package uk.gov.hmcts.reform.timedevent.domain.entities.ccd;

public class EventNotFoundException extends IllegalArgumentException {

    public EventNotFoundException(String message) {
        super(message);
    }
}
