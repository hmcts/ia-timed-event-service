package uk.gov.hmcts.reform.timedevent.infrastructure.controllers.advice;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ErrorResponseTest {

    @Test
    void should_create_error_response_with_builder() {
        Instant now = Instant.now();

        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorCode("BAD_REQUEST")
            .message("Invalid input")
            .timestamp(now)
            .requestId("test-request-id")
            .path("/timed-event")
            .build();

        assertEquals("BAD_REQUEST", errorResponse.getErrorCode());
        assertEquals("Invalid input", errorResponse.getMessage());
        assertEquals(now, errorResponse.getTimestamp());
        assertEquals("test-request-id", errorResponse.getRequestId());
        assertEquals("/timed-event", errorResponse.getPath());
        assertNull(errorResponse.getFieldErrors());
    }

    @Test
    void should_create_error_response_with_field_errors() {
        List<ErrorResponse.FieldError> fieldErrors = List.of(
            ErrorResponse.FieldError.builder()
                .field("caseId")
                .message("Case ID is required")
                .build(),
            ErrorResponse.FieldError.builder()
                .field("event")
                .message("Event is required")
                .build()
        );

        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorCode("VALIDATION_ERROR")
            .message("Validation failed")
            .timestamp(Instant.now())
            .requestId("test-request-id")
            .path("/timed-event")
            .fieldErrors(fieldErrors)
            .build();

        assertNotNull(errorResponse.getFieldErrors());
        assertEquals(2, errorResponse.getFieldErrors().size());
        assertEquals("caseId", errorResponse.getFieldErrors().get(0).getField());
        assertEquals("Case ID is required", errorResponse.getFieldErrors().get(0).getMessage());
    }

    @Test
    void should_allow_null_values() {
        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorCode("ERROR")
            .message("Message")
            .build();

        assertNull(errorResponse.getTimestamp());
        assertNull(errorResponse.getRequestId());
        assertNull(errorResponse.getPath());
        assertNull(errorResponse.getFieldErrors());
    }

    @Test
    void should_create_field_error_with_builder() {
        ErrorResponse.FieldError fieldError = ErrorResponse.FieldError.builder()
            .field("scheduledDateTime")
            .message("Must be in the future")
            .build();

        assertEquals("scheduledDateTime", fieldError.getField());
        assertEquals("Must be in the future", fieldError.getMessage());
    }
}
