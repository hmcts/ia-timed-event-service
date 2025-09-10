package uk.gov.hmcts.reform.timedevent;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.http.Header;
import io.restassured.response.Response;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.timedevent.testutils.FunctionalTest;

public class AuthenticationFunctionTest extends FunctionalTest {

    private String userToken;
    private String s2sToken;

    @BeforeEach
    public void createCase() {

        userToken = idamAuthProvider.getCaseOfficerToken();
        s2sToken = s2sAuthTokenGenerator.generate();
    }

    @Test
    public void should_return_401_when_both_auth_headers_are_missing() {

        Response response = given(requestSpecification)
            .when()
            .contentType("application/json")
            .body(someTimedEventJsonBody())
            .post("/timed-event")
            .then()
            .extract().response();

        assertThat(response.getStatusCode()).isEqualTo(401);

    }

    @Test
    public void should_return_401_when_only_s2s_header_is_set() {

        Response response = given(requestSpecification)
            .when()
            .header(new Header("ServiceAuthorization", s2sToken))
            .contentType("application/json")
            .body(someTimedEventJsonBody())
            .post("/timed-event")
            .then()
            .extract().response();

        assertThat(response.getStatusCode()).isEqualTo(401);

    }

    @Test
    public void should_return_401_when_only_user_auth_header_is_set() {

        Response response = given(requestSpecification)
            .when()
            .header(new Header("Authorization", userToken))
            .contentType("application/json")
            .body(someTimedEventJsonBody())
            .post("/timed-event")
            .then()
            .extract().response();

        assertThat(response.getStatusCode()).isEqualTo(401);

    }

    @Test
    public void should_return_401_when_both_headers_are_set_and_user_auth_header_is_invalid() {

        Response response = given(requestSpecification)
            .when()
            .header(new Header("Authorization", userToken.replace("Bearer ", "")))
            .header(new Header("ServiceAuthorization", s2sToken))
            .contentType("application/json")
            .body(someTimedEventJsonBody())
            .post("/timed-event")
            .then()
            .extract().response();

        assertThat(response.getStatusCode()).isEqualTo(401);

    }

    @Test
    public void should_return_401_when_both_headers_are_set_and_s2s_header_is_invalid() {

        Response response = given(requestSpecification)
            .when()
            .header(new Header("Authorization", userToken))
            .header(new Header("ServiceAuthorization", s2sToken + "abc"))
            .contentType("application/json")
            .body(someTimedEventJsonBody())
            .post("/timed-event")
            .then()
            .extract().response();

        assertThat(response.getStatusCode()).isEqualTo(401);

    }

    @Test
    public void should_return_201_when_both_headers_are_set_and_they_are_both_valid() {

        Response response = given(requestSpecification)
            .when()
            .header(new Header("Authorization", userToken))
            .header(new Header("ServiceAuthorization", s2sToken))
            .contentType("application/json")
            .body(someTimedEventJsonBody())
            .post("/timed-event")
            .then()
            .extract().response();

        assertThat(response.getStatusCode()).isEqualTo(201);

    }

    private String someTimedEventJsonBody() {

        return "{ \"jurisdiction\": \"IA\","
               + "\"caseType\": \"Asylum\","
               + "\"caseId\": 12345,"
               + "\"event\": \"requestHearingRequirementsFeature\","
               + "\"scheduledDateTime\": \"" + ZonedDateTime.now().plusDays(3).toString() + "\""
               + "}";
    }
}


