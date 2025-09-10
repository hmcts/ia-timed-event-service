package uk.gov.hmcts.reform.timedevent.infrastructure.controllers.advice;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.SchedulerException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import uk.gov.hmcts.reform.timedevent.domain.entities.TimedEvent;
import uk.gov.hmcts.reform.timedevent.infrastructure.services.exceptions.SchedulerProcessingException;

@ExtendWith(MockitoExtension.class)
class SchedulerExceptionHandlerControllerAdviceTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private RequestAttributes requestAttributes;

    @BeforeEach
    public void setUo() {
        RequestContextHolder.setRequestAttributes(requestAttributes);
    }

    @Test
    public void should_return_internal_server_error_when_scheduler_processing_exception() {

        when(requestAttributes.getAttribute("CCDCaseId", RequestAttributes.SCOPE_REQUEST)).thenReturn(12345);

        SchedulerExceptionHandlerControllerAdvice controllerAdvice = new SchedulerExceptionHandlerControllerAdvice();

        ResponseEntity<TimedEvent> responseEntity = controllerAdvice.handleExceptions(
            request,
            new SchedulerProcessingException(new SchedulerException())
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertNull(responseEntity.getBody());
    }

    @AfterEach
    public void cleanUp() {
        RequestContextHolder.resetRequestAttributes();
    }
}
