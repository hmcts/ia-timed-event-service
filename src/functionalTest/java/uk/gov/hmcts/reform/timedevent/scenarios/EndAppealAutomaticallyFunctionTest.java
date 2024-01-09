package uk.gov.hmcts.reform.timedevent.scenarios;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import feign.FeignException;
import io.restassured.http.Header;
import io.restassured.response.Response;
import java.time.ZonedDateTime;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.timedevent.testutils.FunctionalTest;
import uk.gov.hmcts.reform.timedevent.testutils.data.CaseDataFixture;

@Slf4j
public class EndAppealAutomaticallyFunctionTest extends FunctionalTest {

    private String jurisdiction = "IA";
    private String caseType = "Asylum";
    private String event = "endAppealAutomatically";

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

    }

    @Test
    public void should_trigger_endAppealAutomatically_event() {

        long caseId = caseDataFixture.getCaseId();
        Response response = null;
        for (int i = 0; i < 5; i ++) {
            try {
                // execute Timed Event now
                response = scheduleEventNow(caseId);
                break;
            } catch (FeignException fe) {
                log.error("Response returned error with " + fe.getMessage() + ". Retrying test.");
            }
        }
        assertNotNull(response);
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
