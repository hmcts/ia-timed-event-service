package uk.gov.hmcts.reform.timedevent.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.timedevent.testutils.SpringBootIntegrationTest;

/**
 * Built-in feature which saves service's swagger specs in temporary directory.
 * Each travis run on master should automatically save and upload (if updated) documentation.
 */
class SwaggerPublisher extends SpringBootIntegrationTest {

    @DisplayName("Generate swagger documentation")
    @Test
    void generateDocs() throws Exception {
        byte[] specs = mockMvc
            .perform(get("/v2/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        try (OutputStream outputStream = Files.newOutputStream(Paths.get("/tmp/swagger-specs.json"))) {
            outputStream.write(specs);
        }

    }
}
