package uk.gov.hmcts.reform.timedevent.infrastructure.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.timedevent.infrastructure.services.IdentityProvider;
import uk.gov.hmcts.reform.timedevent.testutils.SpringBootIntegrationTest;

public class PostTimedEventIntegrationTest extends SpringBootIntegrationTest {

    @MockBean
    IdentityProvider identityProvider;

    @Test
    @WithMockUser(authorities = {"caseworker-ia-caseofficer"})
    public void timedEventEndpoint() throws Exception {

        String identity = "cee88160-78f4-4d24-87ff-91fd3b131034";

        when(identityProvider.identity()).thenReturn(identity);

        // schedule timed event
        MvcResult postResponse = mockMvc
            .perform(
                post("/timed-event")
                    .content(timedEvent())
                    .contentType("application/json")
            )
            .andExpect(status().isCreated())
            .andReturn();

        assertEquals(timedEventResponse(identity), postResponse.getResponse().getContentAsString());

        // assert creation of timed event
        MvcResult getResponse = mockMvc
            .perform(
                get("/timed-event/" + identity)
                    .content(timedEvent())
                    .contentType("application/json")
            )
            .andExpect(status().isOk())
            .andReturn();

        assertEquals(timedEventResponse(identity), getResponse.getResponse().getContentAsString());
    }

    private String timedEventResponse(String id) {
        return "{\"id\":\"" + id + "\","
               + "\"event\":\"example\","
               + "\"scheduledDateTime\":\"2030-05-12T10:00:00Z\","
               + "\"jurisdiction\":\"IA\","
               + "\"caseType\":\"Asylum\","
               + "\"caseId\":1588772172174023"
               + "}";
    }

    private String timedEvent() {
        return "{\"event\":\"example\","
               + "\"scheduledDateTime\":\"2030-05-12T10:00:00Z\","
               + "\"jurisdiction\":\"IA\","
               + "\"caseType\":\"Asylum\","
               + "\"caseId\":1588772172174023"
               + "}";
    }
}
