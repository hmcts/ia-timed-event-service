package uk.gov.hmcts.reform.timedevent.infrastructure.services;

import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.timedevent.infrastructure.clients.CcdApi;
import uk.gov.hmcts.reform.timedevent.infrastructure.clients.model.ccd.CaseDataContent;
import uk.gov.hmcts.reform.timedevent.infrastructure.clients.model.ccd.CaseDetails;
import uk.gov.hmcts.reform.timedevent.infrastructure.clients.model.ccd.Event;
import uk.gov.hmcts.reform.timedevent.infrastructure.clients.model.ccd.StartEventTrigger;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.entities.EventExecution;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.services.EventExecutor;
import uk.gov.hmcts.reform.timedevent.infrastructure.security.SystemTokenGenerator;
import uk.gov.hmcts.reform.timedevent.infrastructure.security.SystemUserProvider;
import uk.gov.hmcts.reform.timedevent.infrastructure.security.oauth2.IdentityManagerResponseException;

@Slf4j
@Service
public class CcdEventExecutor implements EventExecutor {

    private final SystemTokenGenerator systemTokenGenerator;

    private final SystemUserProvider systemUserProvider;

    private final AuthTokenGenerator s2sAuthTokenGenerator;

    private final CcdApi ccdApi;

    public CcdEventExecutor(
        SystemTokenGenerator systemTokenGenerator,
        SystemUserProvider systemUserProvider,
        AuthTokenGenerator s2sAuthTokenGenerator,
        CcdApi ccdApi
    ) {
        this.systemTokenGenerator = systemTokenGenerator;
        this.systemUserProvider = systemUserProvider;
        this.s2sAuthTokenGenerator = s2sAuthTokenGenerator;
        this.ccdApi = ccdApi;
    }

    @Override
    public void execute(EventExecution execution) {

        String event = execution.getEvent().toString();
        String caseId = String.valueOf(execution.getCaseId());
        String jurisdiction = execution.getJurisdiction();
        String caseType = execution.getCaseType();

        log.info("Execution event: {}, for case id: {} has been started.", event, caseId);

        String userToken;
        String s2sToken;
        String uid;
        try {
            userToken = "Bearer " + systemTokenGenerator.generate();
            log.info("System user token has been generated for event: {}, caseId: {}.", event, caseId);

            // returned token is already with Bearer prefix
            s2sToken = s2sAuthTokenGenerator.generate();
            log.info("S2S token has been generated for event: {}, caseId: {}.", event, caseId);

            uid = systemUserProvider.getSystemUserId(userToken);
            log.info("System user id has been fetched for event: {}, caseId: {}.", event, caseId);

        } catch (Exception e) {
            throw new IdentityManagerResponseException(e.getMessage(), e);
        }

        StartEventTrigger startEventResponse = ccdApi.startEvent(
            userToken,
            s2sToken,
            uid,
            jurisdiction,
            caseType,
            caseId,
            event
        );

        log.info(
            "Execution token generated for event: {}, for case id: {}. Token: {}",
            event,
            execution.getCaseId(),
            startEventResponse.getToken()
        );

        CaseDetails caseDetails = ccdApi.submitEvent(
            userToken,
            s2sToken,
            uid,
            jurisdiction,
            caseType,
            caseId,
            new CaseDataContent(
                new Event(event, event, event),
                startEventResponse.getToken(),
                true,
                Collections.emptyMap()
            )
        );

        log.info(
            "Event: {}, for case id: {} has been executed. Case state: {}",
            event,
            caseId,
            caseDetails.getState()
        );
    }
}
