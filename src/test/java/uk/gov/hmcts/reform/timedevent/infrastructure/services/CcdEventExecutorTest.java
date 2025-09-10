package uk.gov.hmcts.reform.timedevent.infrastructure.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import feign.FeignException;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.timedevent.infrastructure.clients.CcdApi;
import uk.gov.hmcts.reform.timedevent.infrastructure.clients.model.ccd.CaseDataContent;
import uk.gov.hmcts.reform.timedevent.infrastructure.clients.model.ccd.CaseDetails;
import uk.gov.hmcts.reform.timedevent.infrastructure.clients.model.ccd.StartEventTrigger;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.entities.EventExecution;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.timedevent.infrastructure.security.SystemTokenGenerator;
import uk.gov.hmcts.reform.timedevent.infrastructure.security.SystemUserProvider;
import uk.gov.hmcts.reform.timedevent.infrastructure.security.oauth2.IdentityManagerResponseException;

@ExtendWith(MockitoExtension.class)
class CcdEventExecutorTest {

    @Mock
    private CcdApi ccdApi;

    @Mock
    private SystemTokenGenerator systemTokenGenerator;

    @Mock
    private SystemUserProvider systemUserProvider;

    @Mock
    private AuthTokenGenerator s2sAuthTokenGenerator;

    @Mock
    private StartEventTrigger startEventTrigger;

    @Mock
    private CaseDetails caseDetails;

    @Test
    public void should_execute_event() {

        String token = "token";
        String serviceToken = "Bearer serviceToken";
        String userId = "userId";

        String jurisdiction = "jurisdiction";
        String caseType = "caseType";
        Event event = Event.EXAMPLE;
        long caseId = 1234;

        String ccdToken = "ccdToken";

        String state = "someState";

        when(systemTokenGenerator.generate()).thenReturn(token);
        when(s2sAuthTokenGenerator.generate()).thenReturn(serviceToken);

        when(systemUserProvider.getSystemUserId("Bearer " + token)).thenReturn(userId);

        when(startEventTrigger.getToken()).thenReturn(ccdToken);
        when(ccdApi.startEvent(
            "Bearer " + token,
            serviceToken,
            userId,
            jurisdiction,
            caseType,
            String.valueOf(caseId),
            event.toString()
        )).thenReturn(startEventTrigger);

        when(caseDetails.getState()).thenReturn(state);
        when(ccdApi.submitEvent(
            eq("Bearer " + token),
            eq(serviceToken),
            eq(userId),
            eq(jurisdiction),
            eq(caseType),
            eq(String.valueOf(caseId)),
            any(CaseDataContent.class))
        ).thenReturn(caseDetails);

        CcdEventExecutor ccdEventExecutor = new CcdEventExecutor(systemTokenGenerator, systemUserProvider, s2sAuthTokenGenerator, ccdApi);

        EventExecution execution = new EventExecution(
            event,
            jurisdiction,
            caseType,
            caseId
        );
        ccdEventExecutor.execute(execution);

        verify(systemTokenGenerator).generate();
        verify(s2sAuthTokenGenerator).generate();

        verify(systemUserProvider).getSystemUserId("Bearer " + token);

        verify(ccdApi).startEvent(
            "Bearer " + token,
            serviceToken,
            userId,
            jurisdiction,
            caseType,
            String.valueOf(caseId),
            event.toString()
        );

        verify(startEventTrigger, times(2)).getToken();

        ArgumentCaptor<CaseDataContent> caseDataCaptor = ArgumentCaptor.forClass(CaseDataContent.class);

        verify(ccdApi).submitEvent(
            eq("Bearer " + token),
            eq(serviceToken),
            eq(userId),
            eq(jurisdiction),
            eq(caseType),
            eq(String.valueOf(caseId)),
            caseDataCaptor.capture()
        );

        assertEquals(event.toString(), caseDataCaptor.getValue().getEvent().getId());
        assertEquals(event.toString(), caseDataCaptor.getValue().getEvent().getDescription());
        assertEquals(event.toString(), caseDataCaptor.getValue().getEvent().getSummary());

        assertEquals(ccdToken, caseDataCaptor.getValue().getEventToken());
        assertTrue(caseDataCaptor.getValue().isIgnoreWarning());
        assertEquals(Collections.<String, Object>emptyMap(), caseDataCaptor.getValue().getData());

        verify(caseDetails).getState();

    }

    @Test
    public void should_return_exception_when_idam_is_not_available() {

        String jurisdiction = "jurisdiction";
        String caseType = "caseType";
        Event event = Event.EXAMPLE;
        long caseId = 1234;

        when(systemTokenGenerator.generate()).thenThrow(new RuntimeException());

        CcdEventExecutor ccdEventExecutor = new CcdEventExecutor(systemTokenGenerator, systemUserProvider, s2sAuthTokenGenerator, ccdApi);

        EventExecution execution = new EventExecution(
            event,
            jurisdiction,
            caseType,
            caseId
        );

        assertThrows(
            IdentityManagerResponseException.class,
            () -> ccdEventExecutor.execute(execution)
        );

        verify(systemTokenGenerator).generate();
    }

    @Test
    public void should_throw_exception_when_s2s_auth_is_not_available() {

        String jurisdiction = "jurisdiction";
        String caseType = "caseType";
        Event event = Event.EXAMPLE;
        long caseId = 1234;

        String token = "token";

        when(systemTokenGenerator.generate()).thenReturn(token);

        when(s2sAuthTokenGenerator.generate()).thenThrow(new RuntimeException());

        CcdEventExecutor ccdEventExecutor = new CcdEventExecutor(systemTokenGenerator, systemUserProvider, s2sAuthTokenGenerator, ccdApi);

        EventExecution execution = new EventExecution(
            event,
            jurisdiction,
            caseType,
            caseId
        );

        assertThrows(
            IdentityManagerResponseException.class,
            () -> ccdEventExecutor.execute(execution)
        );

        verify(systemTokenGenerator).generate();
        verify(s2sAuthTokenGenerator).generate();
    }

    @Test
    public void should_throw_same_exception_as_ccd_api_throws_it() {

        String token = "token";
        String serviceToken = "Bearer serviceToken";
        String userId = "userId";

        String jurisdiction = "jurisdiction";
        String caseType = "caseType";
        Event event = Event.EXAMPLE;
        long caseId = 1234;

        when(systemTokenGenerator.generate()).thenReturn(token);
        when(s2sAuthTokenGenerator.generate()).thenReturn(serviceToken);

        when(systemUserProvider.getSystemUserId("Bearer " + token)).thenReturn(userId);

        when(ccdApi.startEvent(
            "Bearer " + token,
            serviceToken,
            userId,
            jurisdiction,
            caseType,
            String.valueOf(caseId),
            event.toString()
        )).thenThrow(FeignException.class);

        CcdEventExecutor ccdEventExecutor = new CcdEventExecutor(systemTokenGenerator, systemUserProvider, s2sAuthTokenGenerator, ccdApi);

        EventExecution execution = new EventExecution(
            event,
            jurisdiction,
            caseType,
            caseId
        );

        assertThrows(
            FeignException.class,
            () -> ccdEventExecutor.execute(execution)
        );

        verify(systemTokenGenerator).generate();
        verify(s2sAuthTokenGenerator).generate();

        verify(systemUserProvider).getSystemUserId("Bearer " + token);

        verify(ccdApi).startEvent(
            "Bearer " + token,
            serviceToken,
            userId,
            jurisdiction,
            caseType,
            String.valueOf(caseId),
            event.toString()
        );
    }
}
