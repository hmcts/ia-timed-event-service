package uk.gov.hmcts.reform.timedevent.infrastructure.domain.entities.ccd;


import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum Event {

    REQUEST_RESPONDENT_EVIDENCE("requestRespondentEvidence"),
    EXAMPLE("example"),
    REQUEST_HEARING_REQUIREMENTS_FEATURE("requestHearingRequirementsFeature"),
    MOVE_TO_PAYMENT_PENDING("moveToPaymentPending"),
    ROLLBACK_PAYMENT("rollbackPayment"),
    ROLLBACK_PAYMENT_TIMEOUT("rollbackPaymentTimeout"),
    ROLLBACK_PAYMENT_TIMEOUT_TO_PAYMENT_PENDING("rollbackPaymentTimeoutToPaymentPending"),
    END_APPEAL_AUTOMATICALLY("endAppealAutomatically"),
    RE_TRIGGER_WA_TASKS("reTriggerWaTasks"),
    RECORD_REMISSION_REMINDER("recordRemissionReminder"),
    SEND_PAYMENT_REMINDER_NOTIFICATION("sendPaymentReminderNotification"),
    SAVE_NOTIFICATIONS_TO_DATA("saveNotificationsToData"),
    TEST_TIMED_EVENT_SCHEDULE("testTimedEventSchedule"),
    @JsonEnumDefaultValue
    UNKNOWN("unknown");

    @JsonValue
    private final String id;

    Event(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }

    public static Event fromString(String event) {
        return Arrays.stream(Event.values())
            .filter(e -> e.id.equals(event))
            .findAny()
            .orElseThrow(() -> new EventNotFoundException("cannot find event: " + event));
    }
}
