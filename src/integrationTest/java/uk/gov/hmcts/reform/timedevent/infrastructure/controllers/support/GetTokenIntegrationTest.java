package uk.gov.hmcts.reform.timedevent.infrastructure.controllers.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.timedevent.testutils.SpringBootIntegrationTest;
import uk.gov.hmcts.reform.timedevent.testutils.WithIdamStub;

public class GetTokenIntegrationTest extends SpringBootIntegrationTest implements WithIdamStub {

    @BeforeEach
    public void stubRequests() {

        addIdamTokenStub(server);
    }

    @Test
    public void tokenSupportEndpoint() throws Exception {
        MvcResult response = mockMvc
            .perform(get("/testing-support/token"))
            .andExpect(status().isOk())
            .andReturn();

        assertEquals(SYSTEM_USER_TOKEN, response.getResponse().getContentAsString());
    }
}
