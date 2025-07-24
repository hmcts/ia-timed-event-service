package uk.gov.hmcts.reform.timedevent.infrastructure.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.ZonedDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import uk.gov.hmcts.reform.timedevent.domain.entities.TimedEvent;
import uk.gov.hmcts.reform.timedevent.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.timedevent.domain.services.SchedulerService;
import uk.gov.hmcts.reform.timedevent.infrastructure.security.CcdEventAuthorizor;

@ExtendWith(MockitoExtension.class)
class TimedEventControllerTest {

    @Mock
    private CcdEventAuthorizor ccdEventAuthorizor;

    @Mock
    private SchedulerService schedulerService;

    private TimedEventController timedEventController;

    private final TimedEvent timedEvent = new TimedEvent(
        "",
        Event.UNKNOWN,
        ZonedDateTime.now(),
        "jurisdiction",
        "caseType",
        1234
    );

    private final String identity = "someId";

    @Test
    void should_return_scheduled_timed_event_on_post() {

        when(schedulerService.schedule(timedEvent)).thenReturn(identity);

        timedEventController = new TimedEventController(ccdEventAuthorizor, schedulerService);

        ResponseEntity<TimedEvent> response = timedEventController.post(timedEvent);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(timedEvent.getEvent(), response.getBody().getEvent());
        assertEquals(timedEvent.getScheduledDateTime(), response.getBody().getScheduledDateTime());
        assertEquals(timedEvent.getJurisdiction(), response.getBody().getJurisdiction());
        assertEquals(timedEvent.getCaseType(), response.getBody().getCaseType());
        assertEquals(timedEvent.getCaseId(), response.getBody().getCaseId());

        assertEquals(identity, response.getBody().getId());

        verify(ccdEventAuthorizor).throwIfNotAuthorized(timedEvent.getEvent());
        verify(schedulerService).schedule(timedEvent);

    }

    @Test
    void should_return_rescheduled_timed_event_when_id_provided_on_post() {

        TimedEvent timedEvent = new TimedEvent(
            identity,
            Event.UNKNOWN,
            ZonedDateTime.now(),
            "jurisdiction",
            "caseType",
            1234
        );

        when(schedulerService.reschedule(timedEvent)).thenReturn(identity);

        timedEventController = new TimedEventController(ccdEventAuthorizor, schedulerService);

        ResponseEntity<TimedEvent> response = timedEventController.post(timedEvent);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(timedEvent.getEvent(), response.getBody().getEvent());
        assertEquals(timedEvent.getScheduledDateTime(), response.getBody().getScheduledDateTime());
        assertEquals(timedEvent.getJurisdiction(), response.getBody().getJurisdiction());
        assertEquals(timedEvent.getCaseType(), response.getBody().getCaseType());
        assertEquals(timedEvent.getCaseId(), response.getBody().getCaseId());

        assertEquals(identity, response.getBody().getId());

        verify(ccdEventAuthorizor).throwIfNotAuthorized(timedEvent.getEvent());
        verify(schedulerService).reschedule(timedEvent);

    }

    @Test
    void should_return_bad_request_when_event_is_missing_on_post() {

        timedEventController = new TimedEventController(ccdEventAuthorizor, schedulerService);

        ResponseEntity<TimedEvent> response = timedEventController.post(new TimedEvent(
            "",
            null,
            ZonedDateTime.now(),
            "IA",
            "Asylum",
            12345
        ));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        verifyNoInteractions(schedulerService);
        verifyNoInteractions(ccdEventAuthorizor);
    }

    @Test
    void should_return_bad_request_when_scheduledDateTime_is_missing_on_post() {

        timedEventController = new TimedEventController(ccdEventAuthorizor, schedulerService);

        ResponseEntity<TimedEvent> response = timedEventController.post(new TimedEvent(
            "",
            Event.EXAMPLE,
            null,
            "IA",
            "Asylum",
            12345
        ));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        verifyNoInteractions(schedulerService);
        verifyNoInteractions(ccdEventAuthorizor);
    }

    @Test
    void should_return_bad_request_when_jurisdiction_is_missing_on_post() {

        timedEventController = new TimedEventController(ccdEventAuthorizor, schedulerService);

        ResponseEntity<TimedEvent> response = timedEventController.post(new TimedEvent(
            "",
            Event.EXAMPLE,
            ZonedDateTime.now(),
            "",
            "Asylum",
            12345
        ));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        verifyNoInteractions(schedulerService);
        verifyNoInteractions(ccdEventAuthorizor);
    }

    @Test
    void should_return_bad_request_when_caseType_is_missing_on_post() {

        timedEventController = new TimedEventController(ccdEventAuthorizor, schedulerService);

        ResponseEntity<TimedEvent> response = timedEventController.post(new TimedEvent(
            "",
            Event.EXAMPLE,
            ZonedDateTime.now(),
            "IA",
            "",
            12345
        ));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        verifyNoInteractions(schedulerService);
        verifyNoInteractions(ccdEventAuthorizor);
    }

    @Test
    void should_return_bad_request_when_caseId_is_missing_on_post() {

        timedEventController = new TimedEventController(ccdEventAuthorizor, schedulerService);

        ResponseEntity<TimedEvent> response = timedEventController.post(new TimedEvent(
            "",
            Event.EXAMPLE,
            ZonedDateTime.now(),
            "IA",
            "Asylum",
            0
        ));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        verifyNoInteractions(schedulerService);
        verifyNoInteractions(ccdEventAuthorizor);
    }

    @Test
    void should_return_timed_event_on_get() {

        when(schedulerService.get(timedEvent.getId())).thenReturn(Optional.of(timedEvent));

        timedEventController = new TimedEventController(ccdEventAuthorizor, schedulerService);

        ResponseEntity<TimedEvent> response = timedEventController.get(timedEvent.getId());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(timedEvent.getEvent(), response.getBody().getEvent());
        assertEquals(timedEvent.getScheduledDateTime(), response.getBody().getScheduledDateTime());
        assertEquals(timedEvent.getJurisdiction(), response.getBody().getJurisdiction());
        assertEquals(timedEvent.getCaseType(), response.getBody().getCaseType());
        assertEquals(timedEvent.getCaseId(), response.getBody().getCaseId());
        assertEquals(timedEvent.getId(), response.getBody().getId());

        verify(schedulerService).get(timedEvent.getId());
    }

    @Test
    void should_return_not_found_when_when_identity_does_not_exists_on_get() {

        String notExistingIdentity = "notExistingIdentity";
        when(schedulerService.get(notExistingIdentity)).thenReturn(Optional.empty());

        timedEventController = new TimedEventController(ccdEventAuthorizor, schedulerService);

        ResponseEntity<TimedEvent> response = timedEventController.get(notExistingIdentity);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        verify(schedulerService).get(notExistingIdentity);
    }

    @Test
    void should_throw_access_denied_exception_when_ccd_event_authorizor_complains() {

        doThrow(new AccessDeniedException("Event 'unknown' not allowed")).when(ccdEventAuthorizor).throwIfNotAuthorized(Event.UNKNOWN);
        timedEventController = new TimedEventController(ccdEventAuthorizor, schedulerService);

        AccessDeniedException thrown = assertThrows(
            AccessDeniedException.class,
            () -> timedEventController.post(timedEvent)
        );
        assertEquals("Event 'unknown' not allowed", thrown.getMessage());

        verifyNoInteractions(schedulerService);
    }
}
