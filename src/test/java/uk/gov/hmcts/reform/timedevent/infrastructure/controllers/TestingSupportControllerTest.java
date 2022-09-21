package uk.gov.hmcts.reform.timedevent.infrastructure.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import feign.FeignException;
import feign.Request;
import java.util.Collections;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.entities.EventExecution;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.services.EventExecutor;
import uk.gov.hmcts.reform.timedevent.infrastructure.security.SystemTokenGenerator;
import uk.gov.hmcts.reform.timedevent.infrastructure.security.SystemUserProvider;

@ExtendWith(MockitoExtension.class)
class TestingSupportControllerTest {

    @Mock
    private SystemTokenGenerator systemTokenGenerator;

    @Mock
    private SystemUserProvider systemUserProvider;

    @Mock
    private EventExecutor eventExecutor;

    private Request request = Request.create(
        Request.HttpMethod.GET,
        "",
        Collections.emptyMap(),
        null,
        null,
        null
    );

    @Test
    void should_return_token_from_generator() {

        String token = "someUserTokenHash";
        when(systemTokenGenerator.generate()).thenReturn(token);

        TestingSupportController testingSupportController = new TestingSupportController(
            systemTokenGenerator,
            systemUserProvider,
            eventExecutor
        );

        ResponseEntity<String> response = testingSupportController.token();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(token, response.getBody());
    }

    @Test
    void should_return_user_id_from_provider() {

        String token = "someUserTokenHash";
        String userId = "someUserId";
        when(systemTokenGenerator.generate()).thenReturn(token);
        when(systemUserProvider.getSystemUserId("Bearer " + token)).thenReturn(userId);

        TestingSupportController testingSupportController = new TestingSupportController(
            systemTokenGenerator,
            systemUserProvider,
            eventExecutor
        );

        ResponseEntity<String> response = testingSupportController.systemUser();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userId, response.getBody());
    }

    @Test
    void should_execute_event() {

        String jurisdiction = "someJurisdiction";
        String caseType = "someCaseType";
        long caseId = 1234;
        String event = "example";

        TestingSupportController testingSupportController = new TestingSupportController(
            systemTokenGenerator,
            systemUserProvider,
            eventExecutor
        );

        ResponseEntity<String> response = testingSupportController.execute(jurisdiction, caseType, caseId, event);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("event: " + Event.EXAMPLE + ", executed for id: " + caseId, response.getBody());
    }

    @Test
    void should_return_correct_status_when_executor_failed_with_feign_exception() {

        String jurisdiction = "someJurisdiction";
        String caseType = "someCaseType";
        long caseId = 1234;
        String event = "example";

        String message = "someMessage";

        doThrow(new FeignException.BadRequest("", request, message.getBytes(), new HashMap<>())).when(eventExecutor).execute(any(EventExecution.class));

        TestingSupportController testingSupportController = new TestingSupportController(
            systemTokenGenerator,
            systemUserProvider,
            eventExecutor
        );

        ResponseEntity<String> response = testingSupportController.execute(jurisdiction, caseType, caseId, event);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(message, response.getBody());
    }

    @Test
    void should_return_bad_request_when_event_is_wrong() {

        String jurisdiction = "someJurisdiction";
        String caseType = "someCaseType";
        long caseId = 1234;
        String event = "notExistingEvent";

        TestingSupportController testingSupportController = new TestingSupportController(
            systemTokenGenerator,
            systemUserProvider,
            eventExecutor
        );

        ResponseEntity<String> response = testingSupportController.execute(jurisdiction, caseType, caseId, event);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("cannot find event: " + event, response.getBody());
    }

    @Test
    void should_throw_exception_when_executor_failed_with_unknown_exception() {

        String jurisdiction = "someJurisdiction";
        String caseType = "someCaseType";
        long caseId = 1234;
        String event = "example";

        doThrow(RuntimeException.class).when(eventExecutor).execute(any(EventExecution.class));

        TestingSupportController testingSupportController = new TestingSupportController(
            systemTokenGenerator,
            systemUserProvider,
            eventExecutor
        );

        assertThrows(
            RuntimeException.class,
            () -> testingSupportController.execute(jurisdiction, caseType, caseId, event)
        );
    }
}
