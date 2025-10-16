package uk.gov.hmcts.reform.timedevent.scenarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import io.restassured.response.Response;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.timedevent.testutils.FunctionalTest;
import uk.gov.hmcts.reform.timedevent.testutils.data.CaseDataFixture;

@Slf4j
public class RequestHearingRequirementsFunctionTest extends FunctionalTest {

    private String jurisdiction = "IA";
    private String caseType = "Asylum";
    private String event = "requestHearingRequirementsFeature";

    private String systemUserToken;
    private String systemUserId;

    private CaseDataFixture caseDataFixture;

    @BeforeEach
    public void createCase() {

        systemUserToken = idamAuthProvider.getSystemUserToken();
        systemUserId = idamAuthProvider.getUserId(systemUserToken);

        caseDataFixture = new CaseDataFixture(
            ccdApi,
            objectMapper,
            s2sAuthTokenGenerator,
            minimalAppealStarted,
            idamAuthProvider,
            mapValueExpander
        );

        caseDataFixture.startAppeal();
        caseDataFixture.submitAppeal();
        caseDataFixture.requestHomeOfficeData();
        caseDataFixture.requestRespondentEvidence();
        caseDataFixture.uploadRespondentEvidence();
        caseDataFixture.buildCase();
        caseDataFixture.requestRespondentReview();
        caseDataFixture.uploadHomeOfficeAppealResponse();
        caseDataFixture.requestResponseReview();
    }

    @Test
    public void should_trigger_requestHearingRequirementsFeature_event() {

        long caseId = caseDataFixture.getCaseId();
        String auth = caseDataFixture.getCaseOfficerToken();
        String serviceAuth = caseDataFixture.getS2sToken();
        Response response = null;
        for (int i = 0; i < 5; i++) {
            try {
                // execute Timed Event soon
                response = scheduleEventSoon(caseId, auth, serviceAuth, event, jurisdiction, caseType);
                break;
            } catch (Exception fe) {
                log.error("Response returned error with {}. Retrying test.", fe.getMessage());
            }
        }
        assertNotNull(response);
        assertThat(response.getStatusCode()).isEqualTo(201);

        // assert that Timed Event execution changed case state
        assertThatCaseIsInState(caseId, "submitHearingRequirements",
            systemUserToken, systemUserId, jurisdiction, caseType, caseDataFixture);
    }
}
