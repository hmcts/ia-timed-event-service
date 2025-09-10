package uk.gov.hmcts.reform.timedevent.infrastructure.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.timedevent.infrastructure.services.IdentityProvider;
import uk.gov.hmcts.reform.timedevent.testutils.SpringBootIntegrationTest;

class PostTimedEventIntegrationTest extends SpringBootIntegrationTest {

    @MockBean
    IdentityProvider identityProvider;

    @Test
    @WithMockUser(authorities = {"caseworker-ia-caseofficer"})
    void timedEventEndpoint() throws Exception {

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

        assertEquals(timedEventWithId(identity), postResponse.getResponse().getContentAsString());

        // assert creation of timed event
        MvcResult getResponse = mockMvc
            .perform(
                get("/timed-event/" + identity)
                    .contentType("application/json")
            )
            .andExpect(status().isOk())
            .andReturn();

        assertEquals(timedEventWithId(identity), getResponse.getResponse().getContentAsString());

        // change scheduled date time
        postResponse = mockMvc
            .perform(
                post("/timed-event")
                    .content(timedEventWithIdAndDate(identity))
                    .contentType("application/json")
            )
            .andExpect(status().isCreated())
            .andReturn();

        assertEquals(timedEventWithIdAndDate(identity), postResponse.getResponse().getContentAsString());

        // assert changed of timed event
        getResponse = mockMvc
            .perform(
                get("/timed-event/" + identity)
                    .contentType("application/json")
            )
            .andExpect(status().isOk())
            .andReturn();

        assertEquals(timedEventWithIdAndDate(identity), getResponse.getResponse().getContentAsString());
    }

    @Test
    @WithMockUser(authorities = {"caseworker-ia-caseofficer"})
    void deleteTimedEventEndpoint() throws Exception {

        // Given: a task has been scheduled
        String identity = "cee88160-78f4-4d24-87ff-91fd3b131034";

        when(identityProvider.identity()).thenReturn(identity);

        mockMvc.perform(
            post("/timed-event").content(timedEvent()).contentType("application/json")
            )
            .andExpect(status().isCreated())
            .andReturn();

        // When: I try to delete the event
        mockMvc.perform(delete("/timed-event/" + identity).contentType("application/json"))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        // Then: the event is no longer in quartz
        mockMvc.perform(get("/timed-event/" + identity).contentType("application/json"))
            .andExpect(status().isNotFound())
            .andReturn();
    }

    private String timedEventWithIdAndDate(String id) {
        return "{\"id\":\"" + id + "\","
               + "\"event\":\"example\","
               + "\"scheduledDateTime\":\"2030-07-12T10:00:00Z\","
               + "\"jurisdiction\":\"IA\","
               + "\"caseType\":\"Asylum\","
               + "\"caseId\":1588772172174023"
               + "}";
    }

    private String timedEventWithId(String id) {
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
