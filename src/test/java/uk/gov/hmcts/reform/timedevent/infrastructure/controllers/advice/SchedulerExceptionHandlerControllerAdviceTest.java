package uk.gov.hmcts.reform.timedevent.infrastructure.controllers.advice;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.SchedulerException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import uk.gov.hmcts.reform.timedevent.domain.entities.ccd.EventNotFoundException;
import uk.gov.hmcts.reform.timedevent.infrastructure.config.CorrelationIdFilter;
import uk.gov.hmcts.reform.timedevent.infrastructure.security.oauth2.IdentityManagerResponseException;
import uk.gov.hmcts.reform.timedevent.infrastructure.services.exceptions.SchedulerProcessingException;

@ExtendWith(MockitoExtension.class)
class SchedulerExceptionHandlerControllerAdviceTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private ErrorResponseBuilder errorResponseBuilder;

    private SchedulerExceptionHandlerControllerAdvice controllerAdvice;

    @BeforeEach
    void setUp() {
        controllerAdvice = new SchedulerExceptionHandlerControllerAdvice(errorResponseBuilder);
        MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, "test-correlation-id");
        MDC.put(CorrelationIdFilter.CCD_CASE_ID_MDC_KEY, "12345");
    }

    @AfterEach
    void cleanUp() {
        MDC.clear();
    }

    private ErrorResponse createMockErrorResponse(ErrorCode errorCode) {
        return ErrorResponse.builder()
            .errorCode(errorCode.getCode())
            .message(errorCode.getDefaultMessage())
            .timestamp(Instant.now())
            .requestId("test-correlation-id")
            .path("/timed-event")
            .build();
    }

    @Test
    void should_return_internal_server_error_when_scheduler_processing_exception() {
        SchedulerProcessingException ex = new SchedulerProcessingException(new SchedulerException());
        when(errorResponseBuilder.build(eq(ErrorCode.SCHEDULER_ERROR), eq(request), any()))
            .thenReturn(createMockErrorResponse(ErrorCode.SCHEDULER_ERROR));

        ResponseEntity<ErrorResponse> responseEntity = controllerAdvice.handleSchedulerProcessingException(request, ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals("SCHEDULER_ERROR", responseEntity.getBody().getErrorCode());
        verify(errorResponseBuilder).logError(ex, ErrorCode.SCHEDULER_ERROR, request);
    }

    @Test
    void should_return_bad_request_when_event_not_found_exception() {
        EventNotFoundException ex = new EventNotFoundException("Event not found");
        when(errorResponseBuilder.build(eq(ErrorCode.INVALID_EVENT), eq(request), eq("Event not found")))
            .thenReturn(createMockErrorResponse(ErrorCode.INVALID_EVENT));

        ResponseEntity<ErrorResponse> responseEntity = controllerAdvice.handleEventNotFoundException(request, ex);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals("INVALID_EVENT", responseEntity.getBody().getErrorCode());
        verify(errorResponseBuilder).logError(ex, ErrorCode.INVALID_EVENT, request);
    }

    @Test
    void should_return_forbidden_when_access_denied_exception() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");
        when(errorResponseBuilder.build(eq(ErrorCode.ACCESS_DENIED), eq(request), any()))
            .thenReturn(createMockErrorResponse(ErrorCode.ACCESS_DENIED));

        ResponseEntity<ErrorResponse> responseEntity = controllerAdvice.handleAccessDeniedException(request, ex);

        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals("ACCESS_DENIED", responseEntity.getBody().getErrorCode());
        verify(errorResponseBuilder).logError(ex, ErrorCode.ACCESS_DENIED, request);
    }

    @Test
    void should_return_unauthorized_when_identity_manager_exception() {
        IdentityManagerResponseException ex = new IdentityManagerResponseException("Token error", new RuntimeException());
        when(errorResponseBuilder.build(eq(ErrorCode.AUTHENTICATION_ERROR), eq(request), any()))
            .thenReturn(createMockErrorResponse(ErrorCode.AUTHENTICATION_ERROR));

        ResponseEntity<ErrorResponse> responseEntity = controllerAdvice.handleIdentityManagerResponseException(request, ex);

        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals("AUTHENTICATION_ERROR", responseEntity.getBody().getErrorCode());
        verify(errorResponseBuilder).logError(ex, ErrorCode.AUTHENTICATION_ERROR, request);
    }

    @Test
    void should_return_bad_request_when_illegal_argument_exception() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");
        when(errorResponseBuilder.build(eq(ErrorCode.BAD_REQUEST), eq(request), eq("Invalid argument")))
            .thenReturn(createMockErrorResponse(ErrorCode.BAD_REQUEST));

        ResponseEntity<ErrorResponse> responseEntity = controllerAdvice.handleIllegalArgumentException(request, ex);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals("BAD_REQUEST", responseEntity.getBody().getErrorCode());
        verify(errorResponseBuilder).logError(ex, ErrorCode.BAD_REQUEST, request);
    }

    @Test
    void should_return_internal_server_error_for_generic_exception() {
        RuntimeException ex = new RuntimeException("Unexpected error");
        when(errorResponseBuilder.build(eq(ErrorCode.INTERNAL_ERROR), eq(request), any()))
            .thenReturn(createMockErrorResponse(ErrorCode.INTERNAL_ERROR));

        ResponseEntity<ErrorResponse> responseEntity = controllerAdvice.handleAllOtherExceptions(request, ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals("INTERNAL_ERROR", responseEntity.getBody().getErrorCode());
        verify(errorResponseBuilder).logError(ex, ErrorCode.INTERNAL_ERROR, request);
    }

    @Test
    void should_return_not_found_status_for_feign_404_exception() {
        Request feignRequest = Request.create(
            Request.HttpMethod.GET,
            "/test",
            Collections.emptyMap(),
            null,
            new RequestTemplate()
        );
        FeignException feignException = FeignException.errorStatus(
            "test",
            feign.Response.builder()
                .status(404)
                .reason("Not Found")
                .request(feignRequest)
                .headers(Collections.emptyMap())
                .build()
        );
        when(errorResponseBuilder.build(eq(ErrorCode.NOT_FOUND), eq(request), any()))
            .thenReturn(createMockErrorResponse(ErrorCode.NOT_FOUND));

        ResponseEntity<ErrorResponse> responseEntity = controllerAdvice.handleFeignException(request, feignException);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals("NOT_FOUND", responseEntity.getBody().getErrorCode());
        verify(errorResponseBuilder).logError(feignException, ErrorCode.NOT_FOUND, request);
    }

    @Test
    void should_return_bad_request_for_feign_400_exception() {
        Request feignRequest = Request.create(
            Request.HttpMethod.GET,
            "/test",
            Collections.emptyMap(),
            null,
            new RequestTemplate()
        );
        FeignException feignException = FeignException.errorStatus(
            "test",
            feign.Response.builder()
                .status(400)
                .reason("Bad Request")
                .request(feignRequest)
                .headers(Collections.emptyMap())
                .build()
        );
        when(errorResponseBuilder.build(eq(ErrorCode.BAD_REQUEST), eq(request), any()))
            .thenReturn(createMockErrorResponse(ErrorCode.BAD_REQUEST));

        ResponseEntity<ErrorResponse> responseEntity = controllerAdvice.handleFeignException(request, feignException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        verify(errorResponseBuilder).logError(feignException, ErrorCode.BAD_REQUEST, request);
    }

    @Test
    void should_return_downstream_error_for_feign_500_exception() {
        Request feignRequest = Request.create(
            Request.HttpMethod.GET,
            "/test",
            Collections.emptyMap(),
            null,
            new RequestTemplate()
        );
        FeignException feignException = FeignException.errorStatus(
            "test",
            feign.Response.builder()
                .status(500)
                .reason("Internal Server Error")
                .request(feignRequest)
                .headers(Collections.emptyMap())
                .build()
        );
        when(errorResponseBuilder.build(eq(ErrorCode.DOWNSTREAM_ERROR), eq(request), any()))
            .thenReturn(createMockErrorResponse(ErrorCode.DOWNSTREAM_ERROR));

        ResponseEntity<ErrorResponse> responseEntity = controllerAdvice.handleFeignException(request, feignException);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        verify(errorResponseBuilder).logError(feignException, ErrorCode.DOWNSTREAM_ERROR, request);
    }
}
