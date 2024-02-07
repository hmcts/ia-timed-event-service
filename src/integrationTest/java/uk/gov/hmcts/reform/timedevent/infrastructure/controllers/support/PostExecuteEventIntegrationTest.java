package uk.gov.hmcts.reform.timedevent.infrastructure.controllers.support;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.timedevent.testutils.SpringBootIntegrationTest;
import uk.gov.hmcts.reform.timedevent.testutils.WithIdamStub;
import uk.gov.hmcts.reform.timedevent.testutils.WithServiceAuthStub;

class PostExecuteEventIntegrationTest extends SpringBootIntegrationTest implements WithIdamStub, WithServiceAuthStub {

    private final String event = "example";
    private final String caseId = "1234";
    private final String jurisdiction = "someJurisdiction";
    private final String caseType = "someCaseType";

    @Test
    void executionEndpoint() throws Exception {

        addIdamTokenStub(server);
        addUserInfoStub(server);
        addServiceAuthStub(server);

        String ccdStartUrl = "/ccd/caseworkers/" + USER_ID
                             + "/jurisdictions/" + jurisdiction
                             + "/case-types/" + caseType
                             + "/cases/" + caseId
                             + "/event-triggers/" + event
                             + "/token?ignore-warning=true";

        String eventToken = "eventToken";
        server.addStubMapping(
            new StubMapping(
                newRequestPattern(RequestMethod.GET, urlEqualTo(ccdStartUrl))
                    .build(),
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"token\": \"" + eventToken + "\", \"eventId\": \"" + event + "\"}")
                    .build()
            )
        );

        String ccdSubmitUrl = "/ccd/caseworkers/" + USER_ID
                              + "/jurisdictions/" + jurisdiction
                              + "/case-types/" + caseType
                              + "/cases/" + caseId
                              + "/events";

        String state = "someState";
        server.addStubMapping(
            new StubMapping(
                newRequestPattern(RequestMethod.POST, urlEqualTo(ccdSubmitUrl))
                    .build(),
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"id\": \"" + caseId + "\", \"state\": \"" + state + "\"}")
                    .build()
            )
        );

        String url = "/testing-support/execute/jurisdiction/" + jurisdiction + "/case-type/" + caseType + "/cid/" + caseId + "/event/" + event;

        MvcResult response = mockMvc
            .perform(post(url))
            .andExpect(status().isOk())
            .andReturn();

        assertEquals("event: " + event + ", executed for id: " + caseId, response.getResponse().getContentAsString());
    }

    @Test
    void should_return_400_when_event_not_found() throws Exception {

        String notExistingEvent = "notExistingEvent";
        String url = String.format("/testing-support/execute/jurisdiction/%s/case-type/%s/cid/%s/event/%s", jurisdiction, caseType, caseId, notExistingEvent);

        MvcResult response = mockMvc
            .perform(post(url))
            .andExpect(status().isBadRequest())
            .andReturn();

        assertThat(response.getResponse().getContentAsString()).contains("cannot find event: " + notExistingEvent);
    }

    @Test
    void should_return_400_when_case_does_not_exists() throws Exception {

        addIdamTokenStub(server);
        addUserInfoStub(server);
        addServiceAuthStub(server);

        String ccdStartUrl = "/ccd/caseworkers/" + USER_ID
                             + "/jurisdictions/" + jurisdiction
                             + "/case-types/" + caseType
                             + "/cases/" + caseId
                             + "/event-triggers/" + event
                             + "/token?ignore-warning=true";

        server.addStubMapping(
            new StubMapping(
                newRequestPattern(RequestMethod.GET, urlEqualTo(ccdStartUrl))
                    .build(),
                aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"exception\":\"uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException\","
                              + "\"timestamp\":\"2020-06-01T13:45:53.073\","
                              + "\"status\":400,"
                              + "\"error\":\"Bad Request\","
                              + "\"message\":\"Case reference is not valid\","
                              + "\"path\":\"/caseworkers/036ed6b6-38d3-44a6-ac84-2a9d5ace1a6c/jurisdictions/IA/case-types/Asylum/cases/123456789/event-triggers/example/token\","
                              + "\"details\":null,"
                              + "\"callbackErrors\":null,"
                              + "\"callbackWarnings\":null}"
                    )
                    .build()
            )
        );

        String url = String.format("/testing-support/execute/jurisdiction/%s/case-type/%s/cid/%s/event/%s", jurisdiction, caseType, caseId, event);

        MvcResult response = mockMvc
            .perform(post(url))
            .andExpect(status().isBadRequest())
            .andReturn();

        assertThat(response.getResponse().getContentAsString()).contains("Case reference is not valid");
    }

    @Test
    void should_return_404_when_event_is_not_defined_in_ccd() throws Exception {

        addIdamTokenStub(server);
        addUserInfoStub(server);
        addServiceAuthStub(server);

        String eventNotExistingInCcd = "example";

        String ccdStartUrl = "/ccd/caseworkers/" + USER_ID
                             + "/jurisdictions/" + jurisdiction
                             + "/case-types/" + caseType
                             + "/cases/" + caseId
                             + "/event-triggers/" + eventNotExistingInCcd
                             + "/token?ignore-warning=true";

        server.addStubMapping(
            new StubMapping(
                newRequestPattern(RequestMethod.GET, urlEqualTo(ccdStartUrl))
                    .build(),
                aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"exception\":\"uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException\","
                              + "\"timestamp\":\"2020-06-01T13:47:11.25\","
                              + "\"status\":404,"
                              + "\"error\":\"Not Found\","
                              + "\"message\":\"Cannot find event unknown for case type Asylum\","
                              + "\"path\":\"/caseworkers/036ed6b6-38d3-44a6-ac84-2a9d5ace1a6c/jurisdictions/IA/case-types/Asylum/cases/1591019229213510/event-triggers/unknown/token\","
                              + "\"details\":null,"
                              + "\"callbackErrors\":null,"
                              + "\"callbackWarnings\":null}"
                    )
                    .build()
            )
        );

        String url = String.format("/testing-support/execute/jurisdiction/%s/case-type/%s/cid/%s/event/%s", jurisdiction, caseType, caseId, eventNotExistingInCcd);

        MvcResult response = mockMvc
            .perform(post(url))
            .andExpect(status().isNotFound())
            .andReturn();

        assertThat(response.getResponse().getContentAsString()).contains("Cannot find event unknown for case type Asylum");
    }

    @Test
    void should_return_504_when_system_user_does_not_have_access_in_ccd_or_case_api_is_down() throws Exception {

        addIdamTokenStub(server);
        addUserInfoStub(server);
        addServiceAuthStub(server);

        String ccdStartUrl = "/ccd/caseworkers/" + USER_ID
                             + "/jurisdictions/" + jurisdiction
                             + "/case-types/" + caseType
                             + "/cases/" + caseId
                             + "/event-triggers/" + event
                             + "/token?ignore-warning=true";

        server.addStubMapping(
            new StubMapping(
                newRequestPattern(RequestMethod.GET, urlEqualTo(ccdStartUrl))
                    .build(),
                aResponse()
                    .withStatus(504)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"exception\":\"uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException\","
                              + "\"timestamp\":\"2020-06-01T13:48:37.603\","
                              + "\"status\":504,\"error\":\"Gateway Timeout\","
                              + "\"message\":\"Callback to service has been unsuccessful for event Request respondent evidence\","
                              + "\"path\":\"/caseworkers/036ed6b6-38d3-44a6-ac84-2a9d5ace1a6c/jurisdictions/IA/case-types/Asylum/cases/1591019307018724/event-triggers/requestRespondentEvidence/token\","
                              + "\"details\":null,"
                              + "\"callbackErrors\":null,"
                              + "\"callbackWarnings\":null}"
                    )
                    .build()
            )
        );

        String url = String.format("/testing-support/execute/jurisdiction/%s/case-type/%s/cid/%s/event/%s", jurisdiction, caseType, caseId, event);

        MvcResult response = mockMvc
            .perform(post(url))
            .andExpect(status().isGatewayTimeout())
            .andReturn();

        assertThat(response.getResponse().getContentAsString()).contains("Callback to service has been unsuccessful for event Request respondent evidence");
    }

    @Test
    void should_return_422_when_case_is_in_wrong_state() throws Exception {

        addIdamTokenStub(server);
        addUserInfoStub(server);
        addServiceAuthStub(server);

        String ccdStartUrl = "/ccd/caseworkers/" + USER_ID
                             + "/jurisdictions/" + jurisdiction
                             + "/case-types/" + caseType
                             + "/cases/" + caseId
                             + "/event-triggers/" + event
                             + "/token?ignore-warning=true";

        server.addStubMapping(
            new StubMapping(
                newRequestPattern(RequestMethod.GET, urlEqualTo(ccdStartUrl))
                    .build(),
                aResponse()
                    .withStatus(422)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"exception\":\"uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException\","
                              + "\"timestamp\":\"2020-06-01T13:49:18.594\","
                              + "\"status\":422,"
                              + "\"error\":\"Unprocessable Entity\","
                              + "\"message\":\"The case status did not qualify for the event\","
                              + "\"path\":\"/caseworkers/036ed6b6-38d3-44a6-ac84-2a9d5ace1a6c/jurisdictions/IA/case-types/Asylum/cases/1591019355059158/event-triggers/requestHearingRequirementsFeature/token\","
                              + "\"details\":null,"
                              + "\"callbackErrors\":null,"
                              + "\"callbackWarnings\":null}"
                    )
                    .build()
            )
        );

        String url = String.format("/testing-support/execute/jurisdiction/%s/case-type/%s/cid/%s/event/%s", jurisdiction, caseType, caseId, event);

        MvcResult response = mockMvc
            .perform(post(url))
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        assertThat(response.getResponse().getContentAsString()).contains("The case status did not qualify for the event");
    }

    @Test
    void should_return_422_when_caseType_is_not_defined_in_ccd() throws Exception {

        addIdamTokenStub(server);
        addUserInfoStub(server);
        addServiceAuthStub(server);

        String notExistingCaseType = "notExistingCaseType";

        String ccdStartUrl = "/ccd/caseworkers/" + USER_ID
                             + "/jurisdictions/" + jurisdiction
                             + "/case-types/" + notExistingCaseType
                             + "/cases/" + caseId
                             + "/event-triggers/" + event
                             + "/token?ignore-warning=true";

        server.addStubMapping(
            new StubMapping(
                newRequestPattern(RequestMethod.GET, urlEqualTo(ccdStartUrl))
                    .build(),
                aResponse()
                    .withStatus(422)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"exception\":\"uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException\","
                              + "\"timestamp\":\"2020-06-01T13:50:11.616\","
                              + "\"status\":422,"
                              + "\"error\":\"Unprocessable Entity\","
                              + "\"message\":\"The case status did not qualify for the event\","
                              + "\"path\":\"/caseworkers/036ed6b6-38d3-44a6-ac84-2a9d5ace1a6c/jurisdictions/IA/case-types/notExistingCaseType/cases/1591019409853879/event-triggers/requestHearingRequirementsFeature/token\","
                              + "\"details\":null,"
                              + "\"callbackErrors\":null,"
                              + "\"callbackWarnings\":null}"
                    )
                    .build()
            )
        );

        String url = String.format("/testing-support/execute/jurisdiction/%s/case-type/%s/cid/%s/event/%s", jurisdiction, notExistingCaseType, caseId, event);

        MvcResult response = mockMvc
            .perform(post(url))
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        assertThat(response.getResponse().getContentAsString()).contains("The case status did not qualify for the event");
    }

    @Test
    void should_return_403_when_jurisdiction_is_not_defined_in_ccd() throws Exception {

        addIdamTokenStub(server);
        addUserInfoStub(server);
        addServiceAuthStub(server);

        String notExistingJurisdiction = "notExistingJurisdiction";

        String ccdStartUrl = "/ccd/caseworkers/" + USER_ID
                             + "/jurisdictions/" + notExistingJurisdiction
                             + "/case-types/" + caseType
                             + "/cases/" + caseId
                             + "/event-triggers/" + event
                             + "/token?ignore-warning=true";

        server.addStubMapping(
            new StubMapping(
                newRequestPattern(RequestMethod.GET, urlEqualTo(ccdStartUrl))
                    .build(),
                aResponse()
                    .withStatus(403)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"timestamp\":\"2020-06-01T13:51:04.363+0000\","
                              + "\"status\":403,"
                              + "\"error\":\"Forbidden\","
                              + "\"message\":\"Access Denied\","
                              + "\"path\":\"/caseworkers/036ed6b6-38d3-44a6-ac84-2a9d5ace1a6c/jurisdictions/notExistingJurisdiction/case-types/Asylum/cases/1591019462509731/event-triggers/requestHearingRequirementsFeature/token\"}"
                    )
                    .build()
            )
        );

        String url = String.format("/testing-support/execute/jurisdiction/%s/case-type/%s/cid/%s/event/%s", notExistingJurisdiction, caseType, caseId, event);

        MvcResult response = mockMvc
            .perform(post(url))
            .andExpect(status().isForbidden())
            .andReturn();

        assertThat(response.getResponse().getContentAsString()).contains("Access Denied");
    }

}
