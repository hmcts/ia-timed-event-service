package uk.gov.hmcts.reform.timedevent.infrastructure.services.quartz;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import uk.gov.hmcts.reform.timedevent.domain.entities.EventExecution;
import uk.gov.hmcts.reform.timedevent.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.timedevent.domain.services.EventExecutor;
import uk.gov.hmcts.reform.timedevent.infrastructure.services.RetryableExceptionHandler;
import uk.gov.hmcts.reform.timedevent.infrastructure.services.exceptions.NonRetryableException;
import uk.gov.hmcts.reform.timedevent.infrastructure.services.exceptions.RetryableException;

@ExtendWith(MockitoExtension.class)
class TimedEventJobTest {

    @Mock
    private EventExecutor eventExecutor;

    @Mock
    private RetryableExceptionHandler exceptionHandler;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @Mock
    private JobDetail jobDetail;

    @Mock
    private JobDataMap jobDataMap;

    private final String jurisdiction = "IA";
    private final String caseType = "Asylum";
    private final long caseId = 12345;

    @BeforeEach
    public void setUp() {
        when(jobDataMap.getString("event")).thenReturn(Event.EXAMPLE.toString());
        when(jobDataMap.getString("jurisdiction")).thenReturn(jurisdiction);
        when(jobDataMap.getString("caseType")).thenReturn(caseType);
        when(jobDataMap.getLong("caseId")).thenReturn(caseId);

        when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);
        when(jobExecutionContext.getJobDetail()).thenReturn(jobDetail);
    }

    @Test
    public void should_execute_job_without_exception_handler_interaction() throws JobExecutionException {
        doNothing().when(eventExecutor).execute(any(EventExecution.class));

        TimedEventJob timedEventJob = new TimedEventJob(eventExecutor, exceptionHandler);

        ArgumentCaptor<EventExecution> execution = ArgumentCaptor.forClass(EventExecution.class);

        timedEventJob.execute(jobExecutionContext);

        verify(eventExecutor).execute(execution.capture());

        assertEquals(Event.EXAMPLE, execution.getValue().getEvent());
        assertEquals(jurisdiction, execution.getValue().getJurisdiction());
        assertEquals(caseType, execution.getValue().getCaseType());
        assertEquals(caseId, execution.getValue().getCaseId());
        verifyNoInteractions(exceptionHandler);
    }

    @Test
    public void should_pass_exception_to_handler_and_rethrow_retryable_when_executor_throws_it() throws JobExecutionException {

        RuntimeException ex = new RuntimeException();
        doThrow(ex).when(eventExecutor).execute(any(EventExecution.class));
        doThrow(new RetryableException()).when(exceptionHandler).wrapException(ex);

        TimedEventJob timedEventJob = new TimedEventJob(eventExecutor, exceptionHandler);

        assertThrows(
            RetryableException.class,
            () ->  timedEventJob.execute(jobExecutionContext)
        );

        verify(eventExecutor).execute(any(EventExecution.class));
        verify(exceptionHandler).wrapException(ex);
    }

    @Test
    public void should_pass_exception_to_handler_and_rethrow_non_retryable_when_executor_throws_it() throws JobExecutionException {

        RuntimeException ex = new RuntimeException();
        doThrow(ex).when(eventExecutor).execute(any(EventExecution.class));
        doThrow(new NonRetryableException()).when(exceptionHandler).wrapException(ex);

        TimedEventJob timedEventJob = new TimedEventJob(eventExecutor, exceptionHandler);

        assertThrows(
            NonRetryableException.class,
            () ->  timedEventJob.execute(jobExecutionContext)
        );

        verify(eventExecutor).execute(any(EventExecution.class));
        verify(exceptionHandler).wrapException(ex);
    }
}
