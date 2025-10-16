package uk.gov.hmcts.reform.timedevent.testutils;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.io.IOException;
import java.time.ZonedDateTime;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.document.DocumentUploadClientApi;
import uk.gov.hmcts.reform.timedevent.infrastructure.clients.IdamApi;
import uk.gov.hmcts.reform.timedevent.infrastructure.config.ServiceTokenGeneratorConfiguration;
import uk.gov.hmcts.reform.timedevent.testutils.clients.*;
import uk.gov.hmcts.reform.timedevent.testutils.data.*;

@SpringBootTest(classes = {
    DocumentUploadClientApiConfiguration.class,
    ServiceTokenGeneratorConfiguration.class,
    FunctionalSpringContext.class
})
@ActiveProfiles("functional")
public class FunctionalTest {

    @Value("${idam.redirectUrl}")
    protected String idamRedirectUrl;
    @Value("${idam.scope}")
    protected String userScope;
    @Value("${spring.security.oauth2.client.registration.oidc.client-id}")
    protected String idamClientId;
    @Value("${spring.security.oauth2.client.registration.oidc.client-secret}")
    protected String idamClientSecret;

    @Value("classpath:templates/minimal-appeal-started.json")
    protected Resource minimalAppealStarted;

    @Autowired
    protected IdamApi idamApi;

    @Autowired
    protected AuthTokenGenerator s2sAuthTokenGenerator;

    @Autowired
    protected ExtendedCcdApi ccdApi;

    @Autowired
    protected DocumentUploadClientApi documentUploadClientApi;

    protected IdamAuthProvider idamAuthProvider;

    @Autowired
    protected ObjectMapper objectMapper;

    protected final String targetInstance =
        StringUtils.defaultIfBlank(
            System.getenv("TEST_URL"),
            "http://localhost:8095"
        );

    protected RequestSpecification requestSpecification;

    protected MapValueExpander mapValueExpander;

    @BeforeEach
    public void setup() throws IOException {
        requestSpecification = new RequestSpecBuilder()
            .setBaseUri(targetInstance)
            .setRelaxedHTTPSValidation()
            .build();

        idamAuthProvider = new IdamAuthProvider(
            idamApi,
            idamRedirectUrl,
            userScope,
            idamClientId,
            idamClientSecret
        );

        DocumentManagementUploader documentManagementUploader = new DocumentManagementUploader(
            documentUploadClientApi,
            idamAuthProvider,
            s2sAuthTokenGenerator
        );

        DocumentManagementFilesFixture documentManagementFilesFixture = new DocumentManagementFilesFixture(documentManagementUploader);
        documentManagementFilesFixture.prepare();

        mapValueExpander = new MapValueExpander(documentManagementFilesFixture);
    }

    protected Response scheduleEventSoon(long caseId, String auth, String serviceAuth, String event, String jurisdiction, String caseType) {
        return given(requestSpecification)
            .when()
            .header(new Header("Authorization", auth))
            .header(new Header("ServiceAuthorization", serviceAuth))
            .contentType("application/json")
            .body("{ \"jurisdiction\": \"" + jurisdiction + "\","
                + " \"caseType\": \"" + caseType + "\","
                + " \"caseId\": " + caseId + ","
                + " \"event\": \"" + event + "\","
                + " \"scheduledDateTime\": \"" + ZonedDateTime.now().plusSeconds(10) + "\" }"
            )
            .post("/timed-event")
            .then()
            .extract().response();
    }

    protected void assertThatCaseIsInState(long caseId, String state, String systemUserToken, String systemUserId,
                                           String jurisdiction, String caseType, CaseDataFixture caseDataFixture) {

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
