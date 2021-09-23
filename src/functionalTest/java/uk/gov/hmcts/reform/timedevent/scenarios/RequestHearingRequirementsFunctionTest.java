package uk.gov.hmcts.reform.timedevent.scenarios;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.restassured.http.Header;
import io.restassured.response.Response;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.timedevent.testutils.FunctionalTest;
import uk.gov.hmcts.reform.timedevent.testutils.data.CaseDataFixture;

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
        systemUserId = idamApi.userInfo(systemUserToken).getUid();

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

        // execute Timed Event now
        Response response = scheduleEventNow(caseId);
        assertThat(response.getStatusCode()).isEqualTo(201);

        // assert that Timed Event execution changed case state
        assertThatCaseIsInState(caseId, "submitHearingRequirements");
    }

    private Response scheduleEventNow(long caseId) {

        return given(requestSpecification)
            .when()
            .header(new Header("Authorization", caseDataFixture.getCaseOfficerToken()))
            .header(new Header("ServiceAuthorization", caseDataFixture.getS2sToken()))
            .contentType("application/json")
            .body("{ \"jurisdiction\": \"" + jurisdiction + "\","
                  + " \"caseType\": \"" + caseType + "\","
                  + " \"caseId\": " + caseId + ","
                  + " \"event\": \"" + event + "\","
                  + " \"scheduledDateTime\": \"" + ZonedDateTime.now().toString() + "\" }"
            )
            .post("/timed-event")
            .then()
            .extract().response();
    }

    private void assertThatCaseIsInState(long caseId, String state) {

        await().pollInterval(2, SECONDS).atMost(60, SECONDS).until(() ->
            ccdApi.get(
                systemUserToken,
                caseDataFixture.getS2sToken(),
                systemUserId,
                jurisdiction,
                caseType,
                String.valueOf(caseId)
            ).getState().equals(state)
        );
    }
}
