package uk.gov.hmcts.reform.timedevent.scenarios;

import io.restassured.http.Header;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.timedevent.testutils.FunctionalTest;
import uk.gov.hmcts.reform.timedevent.testutils.data.CaseDataFixture;

import java.time.ZonedDateTime;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class RetriggerWaTasksFunctionTest extends FunctionalTest {

    private String jurisdiction = "IA";
    private String caseType = "Asylum";
    private String event = "reTriggerWaTasks";

    private String systemUserToken;
    private String systemUserId;

    private CaseDataFixture caseDataFixture;

    @BeforeEach
    public void createCase() {

        systemUserToken = idamAuthProvider.getSystemUserToken();
        systemUserId = idamService.getUserInfo(systemUserToken).getUid();

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

    }

    @Test
    public void should_trigger_reTriggerWaTasks_event() {

        long caseId = caseDataFixture.getCaseId();

        // execute Timed Event now
        Response response = scheduleEventNow(caseId);
        assertThat(response.getStatusCode()).isEqualTo(201);
    }

    private Response scheduleEventNow(long caseId) {

        return given(requestSpecification)
            .when()
            .header(new Header("Authorization", caseDataFixture.getSysUserToken()))
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
}
