package uk.gov.hmcts.reform.timedevent;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.Header;
import io.restassured.response.Response;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.timedevent.domain.entities.TimedEvent;
import uk.gov.hmcts.reform.timedevent.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.timedevent.testutils.FunctionalTest;

public class TimedEventResponseFunctionTest extends FunctionalTest {

    private String userToken;
    private String s2sToken;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void createCase() {

        userToken = idamAuthProvider.getCaseOfficerToken();
        s2sToken = s2sAuthTokenGenerator.generate();
    }

    @Test
    public void should_return_201_and_created_timed_event() throws JsonProcessingException {

        TimedEvent timedEvent = new TimedEvent(
            null,
            Event.EXAMPLE,
            ZonedDateTime.now().plusDays(3),
            "IA",
            "Asylum",
            12345
        );

        String requestBody = objectMapper.writeValueAsString(timedEvent);

        Response response = given(requestSpecification)
            .when()
            .header(new Header("Authorization", userToken))
            .header(new Header("ServiceAuthorization", s2sToken))
            .contentType("application/json")
            .body(requestBody)
            .post("/timed-event")
            .then()
            .extract().response();

        assertThat(response.getStatusCode()).isEqualTo(201);
    }

    @Test
    public void should_return_415_when_content_type_is_not_application_json() throws JsonProcessingException {

        TimedEvent timedEvent = new TimedEvent(
            null,
            Event.EXAMPLE,
            ZonedDateTime.now().plusDays(3),
            "IA",
            "Asylum",
            12345
        );

        String requestBody = objectMapper.writeValueAsString(timedEvent);

        Response response = given(requestSpecification)
            .when()
            .header(new Header("Authorization", userToken))
            .header(new Header("ServiceAuthorization", s2sToken))
            .body(requestBody)
            .post("/timed-event")
            .then()
            .extract().response();

        assertThat(response.getStatusCode()).isEqualTo(415);
    }

    @Test
    public void should_return_400_when_data_is_missing() throws JsonProcessingException {

        TimedEvent timedEvent = new TimedEvent(
            null,
            null,
            ZonedDateTime.now().plusDays(3),
            "IA",
            "Asylum",
            12345
        );

        String requestBody = objectMapper.writeValueAsString(timedEvent);

        Response response = given(requestSpecification)
            .when()
            .header(new Header("Authorization", userToken))
            .header(new Header("ServiceAuthorization", s2sToken))
            .contentType("application/json")
            .body(requestBody)
            .post("/timed-event")
            .then()
            .extract().response();

        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test
    public void should_return_200_when_timed_event_exists() throws JsonProcessingException {

        TimedEvent timedEvent = new TimedEvent(
            null,
            Event.EXAMPLE,
            ZonedDateTime.now().plusDays(3),
            "IA",
            "Asylum",
            12345
        );

        String requestBody = objectMapper.writeValueAsString(timedEvent);

        Response response = given(requestSpecification)
            .when()
            .header(new Header("Authorization", userToken))
            .header(new Header("ServiceAuthorization", s2sToken))
            .contentType("application/json")
            .body(requestBody)
            .post("/timed-event ")
            .then()
            .extract().response();

        assertThat(response.getStatusCode()).isEqualTo(201);

        TimedEvent body = objectMapper.readValue(response.getBody().asString(), TimedEvent.class);

        response = given(requestSpecification)
            .when()
            .header(new Header("Authorization", userToken))
            .header(new Header("ServiceAuthorization", s2sToken))
            .contentType("application/json")
            .get("/timed-event/" + body.getId())
            .then()
            .extract().response();

        assertThat(response.getStatusCode()).isEqualTo(200);

    }

    @Test
    public void should_return_404_when_timed_event_does_not_exist() throws JsonProcessingException {

        Response response = given(requestSpecification)
            .when()
            .header(new Header("Authorization", userToken))
            .header(new Header("ServiceAuthorization", s2sToken))
            .contentType("application/json")
            .get("/timed-event/someNonExistingId")
            .then()
            .extract().response();

        assertThat(response.getStatusCode()).isEqualTo(404);

    }
}


