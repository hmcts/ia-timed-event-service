package uk.gov.hmcts.reform.timedevent.domain.entities.ccd;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.entities.ccd.Event;

class EventTest {

    @Test
    void has_correct_values() {

        assertEquals("requestRespondentEvidence", Event.REQUEST_RESPONDENT_EVIDENCE.toString());
        assertEquals("example", Event.EXAMPLE.toString());
        assertEquals("requestHearingRequirementsFeature", Event.REQUEST_HEARING_REQUIREMENTS_FEATURE.toString());
        assertEquals("moveToPaymentPending", Event.MOVE_TO_PAYMENT_PENDING.toString());
        assertEquals("rollbackPayment", Event.ROLLBACK_PAYMENT.toString());
        assertEquals("rollbackPaymentTimeout", Event.ROLLBACK_PAYMENT_TIMEOUT.toString());
        assertEquals("rollbackPaymentTimeoutToPaymentPending", Event.ROLLBACK_PAYMENT_TIMEOUT_TO_PAYMENT_PENDING.toString());
        assertEquals("endAppealAutomatically", Event.END_APPEAL_AUTOMATICALLY.toString());
        assertEquals("reTriggerWaTasks", Event.RE_TRIGGER_WA_TASKS.toString());
        assertEquals("recordRemissionReminder", Event.RECORD_REMISSION_REMINDER.toString());
        assertEquals("sendPaymentReminderNotification", Event.SEND_PAYMENT_REMINDER_NOTIFICATION.toString());
        assertEquals("saveNotificationsToData", Event.SAVE_NOTIFICATIONS_TO_DATA.toString());
    }

    @Test
    void if_this_test_fails_it_is_because_it_needs_updating_with_your_changes() {
        assertEquals(13, Event.values().length);
    }

}
