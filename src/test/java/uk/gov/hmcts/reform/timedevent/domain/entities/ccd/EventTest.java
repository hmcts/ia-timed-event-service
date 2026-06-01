package uk.gov.hmcts.reform.timedevent.domain.entities.ccd;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class EventTest {
    @ParameterizedTest
    @MethodSource("eventMapping")
    void has_correct_values(String stringValue, Event eventValue) {
        assertEquals(stringValue, eventValue.toString());
        assertEquals(eventValue, Event.fromString(stringValue));
    }

    @Test
    void fromString_should_throw_if_invalid() {
        EventNotFoundException exception = assertThrows(EventNotFoundException.class,
            () -> Event.fromString("invalid"));
        assertEquals("cannot find event: invalid", exception.getMessage());
    }

    @Test
    void if_this_test_fails_it_is_because_eventMapping_needs_updating_with_your_changes() {
        List<Event> eventMappingEvents = eventMapping().map(arg -> arg.get()[1])
            .map(Event.class::cast)
            .toList();
        List<Event> missingEvents = Arrays.stream(Event.values())
            .filter(event -> !eventMappingEvents.contains(event)).toList();
        assertTrue(missingEvents.isEmpty(), "The following events are missing from the eventMapping method: " + missingEvents);
    }

    static Stream<Arguments> eventMapping() {
        return Stream.of(
            Arguments.of("requestRespondentEvidence", Event.REQUEST_RESPONDENT_EVIDENCE),
            Arguments.of("example", Event.EXAMPLE),
            Arguments.of("requestHearingRequirementsFeature", Event.REQUEST_HEARING_REQUIREMENTS_FEATURE),
            Arguments.of("moveToPaymentPending", Event.MOVE_TO_PAYMENT_PENDING),
            Arguments.of("rollbackPayment", Event.ROLLBACK_PAYMENT),
            Arguments.of("rollbackPaymentTimeout", Event.ROLLBACK_PAYMENT_TIMEOUT),
            Arguments.of("rollbackPaymentTimeoutToPaymentPending", Event.ROLLBACK_PAYMENT_TIMEOUT_TO_PAYMENT_PENDING),
            Arguments.of("endAppealAutomatically", Event.END_APPEAL_AUTOMATICALLY),
            Arguments.of("reTriggerWaTasks", Event.RE_TRIGGER_WA_TASKS),
            Arguments.of("recordRemissionReminder", Event.RECORD_REMISSION_REMINDER),
            Arguments.of("sendPaymentReminderNotification", Event.SEND_PAYMENT_REMINDER_NOTIFICATION),
            Arguments.of("saveNotificationsToData", Event.SAVE_NOTIFICATIONS_TO_DATA),
            Arguments.of("saveNotificationsToDataBail", Event.SAVE_NOTIFICATIONS_TO_DATA_BAIL),
            Arguments.of("testTimedEventSchedule", Event.TEST_TIMED_EVENT_SCHEDULE),
            Arguments.of("unknown", Event.UNKNOWN)
        );
    }
}
