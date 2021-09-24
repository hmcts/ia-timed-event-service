package uk.gov.hmcts.reform.timedevent.testutils.data;

import static com.google.common.collect.Lists.newArrayList;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.timedevent.domain.entities.ccd.DynamicList;
import uk.gov.hmcts.reform.timedevent.domain.entities.ccd.Value;
import uk.gov.hmcts.reform.timedevent.infrastructure.clients.model.ccd.CaseDataContent;
import uk.gov.hmcts.reform.timedevent.infrastructure.clients.model.ccd.CaseDetails;
import uk.gov.hmcts.reform.timedevent.infrastructure.clients.model.ccd.Event;
import uk.gov.hmcts.reform.timedevent.infrastructure.clients.model.ccd.StartEventTrigger;
import uk.gov.hmcts.reform.timedevent.testutils.clients.ExtendedCcdApi;

public class CaseDataFixture {

    private final String jurisdiction = "IA";
    private final String caseType = "Asylum";

    private final ObjectMapper objectMapper;
    private final ExtendedCcdApi ccdApi;
    private final AuthTokenGenerator s2sAuthTokenGenerator;
    private final Resource minimalAppealStarted;
    private final IdamAuthProvider idamAuthProvider;
    private final MapValueExpander mapValueExpander;

    private String s2sToken;

    private String legalRepToken;
    private String legalRepUserId;

    private String caseOfficerToken;
    private String caseOfficerUserId;

    private String homeOfficeLartToken;
    private String homeOfficeLartUserId;

    private long caseId;

    public CaseDataFixture(
        ExtendedCcdApi ccdApi,
        ObjectMapper objectMapper,
        AuthTokenGenerator s2sAuthTokenGenerator,
        Resource minimalAppealStarted,
        IdamAuthProvider idamAuthProvider,
        MapValueExpander mapValueExpander
    ) {
        this.ccdApi = ccdApi;
        this.objectMapper = objectMapper;
        this.s2sAuthTokenGenerator = s2sAuthTokenGenerator;
        this.minimalAppealStarted = minimalAppealStarted;
        this.idamAuthProvider = idamAuthProvider;
        this.mapValueExpander = mapValueExpander;
    }

    public String getS2sToken() {
        return s2sToken;
    }

    public String getLegalRepToken() {
        return legalRepToken;
    }

    public String getLegalRepUserId() {
        return legalRepUserId;
    }

    public String getCaseOfficerToken() {
        return caseOfficerToken;
    }

    public String getCaseOfficerUserId() {
        return caseOfficerUserId;
    }

    public String getHomeOfficeLartToken() {
        return homeOfficeLartToken;
    }

    public String getHomeOfficeLartUserId() {
        return homeOfficeLartUserId;
    }

    public long getCaseId() {
        return caseId;
    }

    public void startAppeal() {
        authenticateUsers();

        String event = "startAppeal";
        StartEventTrigger startEventResponse = ccdApi.startCaseCreation(legalRepToken, s2sToken, legalRepUserId, jurisdiction, caseType, event);

        Map<String, Object> data = Collections.emptyMap();
        try {
            data = objectMapper.readValue(
                asString(minimalAppealStarted),
                new TypeReference<Map<String, Object>>() {
                }
            );
        } catch (Exception e) {
            // ignore - test will fail
        }

        mapValueExpander.expandValues(data);
        CaseDataContent content = new CaseDataContent(
            new Event(event, event, event),
            startEventResponse.getToken(),
            true,
            data
        );

        CaseDetails submit = ccdApi.submitCaseCreation(legalRepToken, s2sToken, legalRepUserId, jurisdiction, caseType, content);

        caseId = submit.getId();
    }

    public String submitAppeal() {

        Map<String, Object> noticeOfDecisionDoc = getDocumentToUpload();

        Map<String, Object> data = new HashMap<>();
        data.put("uploadTheNoticeOfDecisionDocs", newArrayList(noticeOfDecisionDoc));
        
        return triggerEvent(
            legalRepToken,
            s2sToken,
            legalRepUserId,
            caseId,
            "submitAppeal",
            data
        );
    }

    public String requestHomeOfficeData() {

        Map<String, Object> data = new HashMap<>();

        final List<Value> values = new ArrayList<>();
        values.add(new Value("NoMatch", "No Match"));
        DynamicList appellantsList = new DynamicList(values.get(0), values);

        data.put("homeOfficeAppellantsList", appellantsList);

        return triggerEvent(
                caseOfficerToken,
                s2sToken,
                caseOfficerUserId,
                caseId,
                "requestHomeOfficeData",
                data
        );

    }
    
    public String requestRespondentEvidence() {

        Map<String, Object> data = new HashMap<>();
        data.put("sendDirectionExplanation", "A notice of appeal has been lodged against this asylum decision.\n"
                                             + "\n"
                                             + "You must now send all documents to the case officer. The case officer will send them to the other party. You have 14 days to supply these documents.\n"
                                             + "\n"
                                             + "You must include:\n"
                                             + "- the notice of decision\n"
                                             + "- any other document provided to the appellant giving reasons for that decision\n"
                                             + "- any statements of evidence\n"
                                             + "- the application form\n"
                                             + "- any record of interview with the appellant in relation to the decision being appealed\n"
                                             + "- any other unpublished documents on which you rely\n"
                                             + "- the notice of any other appealable decision made in relation to the appellant"
        );
        data.put("sendDirectionDateDue", "{$TODAY+14}");
        data.put("sendDirectionParties", "respondent");

        return triggerEvent(
            caseOfficerToken,
            s2sToken,
            caseOfficerUserId,
            caseId,
            "requestRespondentEvidence",
            data
        );
    }

    public String uploadRespondentEvidence() {

        Map<String, Object> respondentEvidence = getDocumentToUpload();

        Map<String, Object> data = new HashMap<>();
        data.put("respondentEvidence", newArrayList(respondentEvidence));

        return triggerEvent(
            caseOfficerToken,
            s2sToken,
            caseOfficerUserId,
            caseId,
            "uploadRespondentEvidence",
            data
        );
    }

    public String buildCase() {

        Map<String, Object> caseArgumentDocument = new HashMap<>();
        caseArgumentDocument.put("document_url", "{$FIXTURE_DOC1_PDF_URL}");
        caseArgumentDocument.put("document_binary_url", "{$FIXTURE_DOC1_PDF_URL_BINARY}");
        caseArgumentDocument.put("document_filename", "{$FIXTURE_DOC1_PDF_FILENAME}");

        Map<String, Object> data = new HashMap<>();
        data.put("caseArgumentDocument", caseArgumentDocument);

        return triggerEvent(
            legalRepToken,
            s2sToken,
            legalRepUserId,
            caseId,
            "buildCase",
            data
        );
    }

    public String submitCase() {

        return triggerEvent(
            legalRepToken,
            s2sToken,
            legalRepUserId,
            caseId,
            "submitCase",
            Collections.emptyMap()
        );
    }

    public String requestRespondentReview() {

        Map<String, Object> data = new HashMap<>();
        data.put("sendDirectionExplanation", "You have 14 days to review the Appeal Skeleton Argument and evidence. You must explain whether the appellant makes a valid case for overturning the original decision.\n"
                                             + "\n"
                                             + "You must respond to the Tribunal and tell them:\n"
                                             + "- whether you oppose all or parts of the appellant's case\n"
                                             + "- what your grounds are for opposing the case\n"
                                             + "- which of the issues are agreed or not agreed\n"
                                             + "- whether there are any further issues you wish to raise\n"
                                             + "- whether you are prepared to withdraw to grant\n"
                                             + "- whether the appeal can be resolved without a hearing\n"
                                             + "\n"
                                             + "# Next steps\n"
                                             + "\n"
                                             + "If you do not respond in time the Tribunal will decide how the case should proceed."
        );
        data.put("sendDirectionDateDue", "{$TODAY+14}");
        data.put("sendDirectionParties", "respondent");

        return triggerEvent(
            caseOfficerToken,
            s2sToken,
            caseOfficerUserId,
            caseId,
            "requestRespondentReview",
            data
        );
    }

    public String uploadHomeOfficeAppealResponse() {

        Map<String, Object> homeOfficeAppealResponseDocument = new HashMap<>();
        homeOfficeAppealResponseDocument.put("document_url", "{$FIXTURE_DOC2_PDF_URL}");
        homeOfficeAppealResponseDocument.put("document_binary_url", "{$FIXTURE_DOC2_PDF_URL_BINARY}");
        homeOfficeAppealResponseDocument.put("document_filename", "{$FIXTURE_DOC2_PDF_FILENAME}");

        Map<String, Object> data = new HashMap<>();
        data.put("homeOfficeAppealResponseDocument", homeOfficeAppealResponseDocument);

        return triggerEvent(
            homeOfficeLartToken,
            s2sToken,
            homeOfficeLartUserId,
            caseId,
            "uploadHomeOfficeAppealResponse",
            data
        );
    }

    // not used for now - clarify why we need it
    public String addAppealResponse() {

        Map<String, Object> appealResponseDocument = new HashMap<>();
        appealResponseDocument.put("document_url", "{$FIXTURE_DOC2_PDF_URL}");
        appealResponseDocument.put("document_binary_url", "{$FIXTURE_DOC2_PDF_URL_BINARY}");
        appealResponseDocument.put("document_filename", "{$FIXTURE_DOC2_PDF_FILENAME}");

        Map<String, Object> data = new HashMap<>();
        data.put("appealResponseDocument", appealResponseDocument);

        return triggerEvent(
            caseOfficerToken,
            s2sToken,
            caseOfficerUserId,
            caseId,
            "addAppealResponse",
            data
        );
    }

    public String requestResponseReview() {

        Map<String, Object> data = new HashMap<>();
        data.put("sendDirectionExplanation", "The Home Office has replied to your Appeal Skeleton Argument and evidence. You should review their response.\n"
                                             + "\n"
                                             + "# Next steps\n"
                                             + "\n"
                                             + "You have 5 days to review the Home Office response. If you want to respond to what they have said, you should email the Tribunal.\n"
                                             + "If you do not respond within 5 days, the case will automatically go to hearing."
        );
        data.put("sendDirectionDateDue", "{$TODAY+5}");
        data.put("sendDirectionParties", "legalRepresentative");

        return triggerEvent(
            caseOfficerToken,
            s2sToken,
            caseOfficerUserId,
            caseId,
            "requestResponseReview",
            data
        );
    }

    protected void authenticateUsers() {
        s2sToken = s2sAuthTokenGenerator.generate();

        legalRepToken = idamAuthProvider.getLegalRepToken();
        legalRepUserId = idamAuthProvider.getUserId(legalRepToken);

        caseOfficerToken = idamAuthProvider.getCaseOfficerToken();
        caseOfficerUserId = idamAuthProvider.getUserId(caseOfficerToken);

        homeOfficeLartToken = idamAuthProvider.getHomeOfficeLartToken();
        homeOfficeLartUserId = idamAuthProvider.getUserId(homeOfficeLartToken);
    }

    private String triggerEvent(String userToken, String s2sToken, String userId, long caseId, String event, Map<String, Object> data) {

        StartEventTrigger startEventResponse = ccdApi.startEvent(userToken, s2sToken, userId, jurisdiction, caseType, String.valueOf(caseId), event);

        mapValueExpander.expandValues(data);

        CaseDataContent content = new CaseDataContent(
            new Event(event, event, event),
            startEventResponse.getToken(),
            true,
            data
        );

        CaseDetails submit = ccdApi.submitEvent(userToken, s2sToken, userId, jurisdiction, caseType, String.valueOf(caseId), content);

        return submit.getState();
    }


    private String asString(Resource resource) {
        try (Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Map<String, Object> getDocumentToUpload() {

        Map<String, Object> doc = new HashMap<>();
        doc.put("document_url", "{$FIXTURE_DOC1_PDF_URL}");
        doc.put("document_binary_url", "{$FIXTURE_DOC1_PDF_URL_BINARY}");
        doc.put("document_filename", "{$FIXTURE_DOC1_PDF_FILENAME}");

        Map<String, Object> document = new HashMap<>();
        document.put("document", doc);
        document.put("description", "Some new evidence");

        Map<String, Object> documentWithId = new HashMap<>();
        documentWithId.put("id", "1");
        documentWithId.put("value", document);

        return documentWithId;
    }
}
