package uk.gov.hmcts.reform.timedevent.infrastructure.controllers.advice;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.reform.timedevent.domain.entities.ccd.EventNotFoundException;
import uk.gov.hmcts.reform.timedevent.infrastructure.security.oauth2.IdentityManagerResponseException;
import uk.gov.hmcts.reform.timedevent.infrastructure.services.exceptions.SchedulerProcessingException;

@RequiredArgsConstructor
@ControllerAdvice(basePackages = "uk.gov.hmcts.reform.timedevent.infrastructure.controllers")
@RequestMapping
public class SchedulerExceptionHandlerControllerAdvice extends ResponseEntityExceptionHandler {

    private final ErrorResponseBuilder errorResponseBuilder;

    @ExceptionHandler(EventNotFoundException.class)
    protected ResponseEntity<ErrorResponse> handleEventNotFoundException(
        HttpServletRequest request, EventNotFoundException ex) {

        errorResponseBuilder.logError(ex, ErrorCode.INVALID_EVENT, request);
        ErrorResponse response = errorResponseBuilder.build(
            ErrorCode.INVALID_EVENT, request, ex.getMessage());
        return new ResponseEntity<>(response, ErrorCode.INVALID_EVENT.getHttpStatus());
    }

    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<ErrorResponse> handleAccessDeniedException(
        HttpServletRequest request, AccessDeniedException ex) {

        errorResponseBuilder.logError(ex, ErrorCode.ACCESS_DENIED, request);
        ErrorResponse response = errorResponseBuilder.build(
            ErrorCode.ACCESS_DENIED, request, null);
        return new ResponseEntity<>(response, ErrorCode.ACCESS_DENIED.getHttpStatus());
    }

    @ExceptionHandler(IdentityManagerResponseException.class)
    protected ResponseEntity<ErrorResponse> handleIdentityManagerResponseException(
        HttpServletRequest request, IdentityManagerResponseException ex) {

        errorResponseBuilder.logError(ex, ErrorCode.AUTHENTICATION_ERROR, request);
        ErrorResponse response = errorResponseBuilder.build(
            ErrorCode.AUTHENTICATION_ERROR, request, null);
        return new ResponseEntity<>(response, ErrorCode.AUTHENTICATION_ERROR.getHttpStatus());
    }

    @ExceptionHandler(FeignException.class)
    protected ResponseEntity<ErrorResponse> handleFeignException(
        HttpServletRequest request, FeignException ex) {

        HttpStatus status = HttpStatus.resolve(ex.status());
        ErrorCode errorCode = mapFeignStatusToErrorCode(status);

        errorResponseBuilder.logError(ex, errorCode, request);
        ErrorResponse response = errorResponseBuilder.build(
            errorCode, request, null);
        return new ResponseEntity<>(response, status != null ? status : errorCode.getHttpStatus());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<ErrorResponse> handleIllegalArgumentException(
        HttpServletRequest request, IllegalArgumentException ex) {

        errorResponseBuilder.logError(ex, ErrorCode.BAD_REQUEST, request);
        ErrorResponse response = errorResponseBuilder.build(
            ErrorCode.BAD_REQUEST, request, ex.getMessage());
        return new ResponseEntity<>(response, ErrorCode.BAD_REQUEST.getHttpStatus());
    }

    @ExceptionHandler(SchedulerProcessingException.class)
    protected ResponseEntity<ErrorResponse> handleSchedulerProcessingException(
        HttpServletRequest request, SchedulerProcessingException ex) {

        errorResponseBuilder.logError(ex, ErrorCode.SCHEDULER_ERROR, request);
        ErrorResponse response = errorResponseBuilder.build(
            ErrorCode.SCHEDULER_ERROR, request, null);
        return new ResponseEntity<>(response, ErrorCode.SCHEDULER_ERROR.getHttpStatus());
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleAllOtherExceptions(
        HttpServletRequest request, Exception ex) {

        errorResponseBuilder.logError(ex, ErrorCode.INTERNAL_ERROR, request);
        ErrorResponse response = errorResponseBuilder.build(
            ErrorCode.INTERNAL_ERROR, request, null);
        return new ResponseEntity<>(response, ErrorCode.INTERNAL_ERROR.getHttpStatus());
    }

    private ErrorCode mapFeignStatusToErrorCode(HttpStatus status) {
        if (status == null) {
            return ErrorCode.DOWNSTREAM_ERROR;
        }
        if (status == HttpStatus.NOT_FOUND) {
            return ErrorCode.NOT_FOUND;
        }
        if (status == HttpStatus.UNAUTHORIZED) {
            return ErrorCode.AUTHENTICATION_ERROR;
        }
        if (status == HttpStatus.FORBIDDEN) {
            return ErrorCode.ACCESS_DENIED;
        }
        if (status.is4xxClientError()) {
            return ErrorCode.BAD_REQUEST;
        }
        return ErrorCode.DOWNSTREAM_ERROR;
    }
}
