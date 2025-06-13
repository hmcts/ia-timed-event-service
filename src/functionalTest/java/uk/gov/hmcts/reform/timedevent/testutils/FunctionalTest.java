package uk.gov.hmcts.reform.timedevent.testutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import java.io.IOException;
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

    protected ObjectMapper objectMapper = new ObjectMapper();
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

        idamAuthProvider = new IdamAuthProvider();

        DocumentManagementUploader documentManagementUploader = new DocumentManagementUploader(
            documentUploadClientApi,
            idamAuthProvider,
            s2sAuthTokenGenerator
        );

        DocumentManagementFilesFixture documentManagementFilesFixture = new DocumentManagementFilesFixture(documentManagementUploader);
        documentManagementFilesFixture.prepare();

        mapValueExpander = new MapValueExpander(documentManagementFilesFixture);

    }

}
