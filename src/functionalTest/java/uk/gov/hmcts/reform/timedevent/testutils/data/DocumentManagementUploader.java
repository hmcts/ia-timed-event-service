package uk.gov.hmcts.reform.timedevent.testutils.data;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.util.Collections;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentUploadRequest;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse;
import uk.gov.hmcts.reform.ccd.document.am.util.InMemoryMultipartFile;
import uk.gov.hmcts.reform.timedevent.testutils.data.model.Document;

public class DocumentManagementUploader {

    private final CaseDocumentClientApi caseDocumentClientApi;
    private final IdamAuthProvider idamAuthProvider;
    private final AuthTokenGenerator s2sAuthTokenGenerator;

    public DocumentManagementUploader(
        CaseDocumentClientApi caseDocumentClientApi,
        IdamAuthProvider idamAuthProvider,
        AuthTokenGenerator s2sAuthTokenGenerator
    ) {
        this.caseDocumentClientApi = caseDocumentClientApi;
        this.idamAuthProvider = idamAuthProvider;
        this.s2sAuthTokenGenerator = s2sAuthTokenGenerator;
    }

    public Document upload(
        Resource resource,
        String classification,
        String caseTypeId,
        String jurisdictionId,
        String contentType
    ) {
        final String serviceAuthorizationToken = s2sAuthTokenGenerator.generate();
        final String accessToken = idamAuthProvider.getLegalRepToken();


        try {

            MultipartFile file = new InMemoryMultipartFile(
                resource.getFilename(),
                resource.getFilename(),
                contentType,
                ByteStreams.toByteArray(resource.getInputStream())
            );

            DocumentUploadRequest documentUploadRequest = new DocumentUploadRequest(classification,
                caseTypeId,jurisdictionId,Collections.singletonList(file));

            UploadResponse uploadResponse =
                caseDocumentClientApi
                    .uploadDocuments(
                        accessToken,
                        serviceAuthorizationToken,
                        documentUploadRequest
                    );

            uk.gov.hmcts.reform.ccd.document.am.model.Document uploadedDocument =  uploadResponse
                    .getDocuments()
                    .get(0);

            return new Document(
                uploadedDocument
                    .links
                    .self
                    .href,
                uploadedDocument
                    .links
                    .binary
                    .href,
                uploadedDocument
                    .originalDocumentName,
                uploadedDocument
                    .hashToken
            );

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
