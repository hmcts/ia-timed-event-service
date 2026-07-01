package uk.gov.hmcts.reform.timedevent.infrastructure.controllers.advice;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 Bad Request
    BAD_REQUEST("BAD_REQUEST", HttpStatus.BAD_REQUEST, "The request contains invalid parameters"),
    INVALID_EVENT("INVALID_EVENT", HttpStatus.BAD_REQUEST, "The requested event could not be found"),
    REQUIRED_FIELD_MISSING("REQUIRED_FIELD_MISSING", HttpStatus.BAD_REQUEST, "Required field is missing"),
    VALIDATION_ERROR("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Validation failed"),

    // 401 Unauthorized
    AUTHENTICATION_ERROR("AUTHENTICATION_ERROR", HttpStatus.UNAUTHORIZED, "Unable to authenticate the request"),

    // 403 Forbidden
    ACCESS_DENIED("ACCESS_DENIED", HttpStatus.FORBIDDEN, "You do not have permission to perform this action"),

    // 404 Not Found
    NOT_FOUND("NOT_FOUND", HttpStatus.NOT_FOUND, "The requested resource was not found"),

    // 500 Internal Server Error
    INTERNAL_ERROR("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"),
    SCHEDULER_ERROR("SCHEDULER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while processing the scheduled event"),
    DOWNSTREAM_ERROR("DOWNSTREAM_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while communicating with a downstream service");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
