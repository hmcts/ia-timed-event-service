package uk.gov.hmcts.reform.timedevent.infrastructure.controllers.advice;

import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.reform.timedevent.domain.entities.TimedEvent;
import uk.gov.hmcts.reform.timedevent.infrastructure.services.exceptions.SchedulerProcessingException;

@Slf4j
@ControllerAdvice(basePackages = "uk.gov.hmcts.reform.timedevent.infrastructure.controllers")
@RequestMapping
public class SchedulerExceptionHandlerControllerAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler({SchedulerProcessingException.class, RuntimeException.class})
    protected ResponseEntity<TimedEvent> handleExceptions(HttpServletRequest request, Exception ex) {
        log.error(
            "Exception for the CCDCaseId: {}, message: {}",
            RequestContextHolder.currentRequestAttributes().getAttribute("CCDCaseId", RequestAttributes.SCOPE_REQUEST),
            ex.getMessage(),
            ex
        );

        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
