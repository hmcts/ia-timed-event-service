package uk.gov.hmcts.reform.timedevent.infrastructure.controllers;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.entities.TimedEvent;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.services.EventExecutor;
import uk.gov.hmcts.reform.timedevent.testutils.SpringBootIntegrationTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext
public class RetryLogicIntegrationTest extends SpringBootIntegrationTest {

    static final long INCREMENT = 250;

    @MockBean
    EventExecutor eventExecutor;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    Scheduler quartzScheduler;

    @Value("#{${retry.durationInSeconds}*1000}")
    long retryIntervalMillis;

    @Value("${retry.maxRetryNumber}")
    int maxRetryNumber;

    @BeforeEach
    @SneakyThrows
    void prepare() {
        quartzScheduler.clear();
        Mockito.reset(eventExecutor);
        weirdSleep((int)(retryIntervalMillis * 1.5f));
    }

    @Test
    @WithMockUser(authorities = {"caseworker-ia-caseofficer"})
    void testScheduledEventHasRunAfterAppropriateTime() {
        // Given: an event scheduled in the future
        scheduleEvent(ZonedDateTime.now().plusSeconds(1), 1588772172174020L);

        // When: I wait for enough time to pass
        weirdSleep(1000); // enough for the original invocation
        weirdSleep(retryIntervalMillis * 2);   // some more time

        // Then: the event is executed
        verify(eventExecutor, times(1)).execute(any());
    }

    @Test
    @WithMockUser(authorities = {"caseworker-ia-caseofficer"})
    void testScheduledEventHasNotRunBeforeTime() {
        // Given: an event scheduled in the future
        scheduleEvent(ZonedDateTime.now().plusSeconds(5), 1588772172174021L);

        // When: I don't wait for enough time to pass
        weirdSleep(1000);

        // Then: the event is not executed
        verify(eventExecutor, times(0)).execute(any());
    }

    @Test
    @WithMockUser(authorities = {"caseworker-ia-caseofficer"})
    void testRetryableExecutionFailureTriggersAnotherAttempt() {
        // Given: an event scheduled in the future that is destined to fail
        doThrow(FeignException.GatewayTimeout.class).when(eventExecutor).execute(any());

        scheduleEvent(ZonedDateTime.now().plusSeconds(1), 1588772172174022L);

        // When: I wait for enough time to pass
        weirdSleep(1000); // enough for the original invocation
        weirdSleep(retryIntervalMillis * 2);  // enough for one more try and then some

        // Then: the event execution is attempted at least twice
        verify(eventExecutor, atLeast(2)).execute(any());
    }

    @Test
    @WithMockUser(authorities = {"caseworker-ia-caseofficer"})
    void testExecutionFailureMaximumAttemptLimitIsRespected() {
        // Given: an event scheduled in the future that is destined to fail
        doThrow(FeignException.GatewayTimeout.class).when(eventExecutor).execute(any());

        scheduleEvent(ZonedDateTime.now().plusSeconds(1), 1588772172174023L);

        // When: I wait for enough time to pass
        weirdSleep(1000); // enough for the original invocation
        weirdSleep(retryIntervalMillis * (maxRetryNumber + 2));  // enough for all the retries plus some

        // Then: the event execution is attempted exactly one time plus the number of retries
        verify(eventExecutor, atLeast(2)).execute(any());
        verify(eventExecutor, atMost(1 + maxRetryNumber)).execute(any());
    }

    @SneakyThrows
    private TimedEvent scheduleEvent(ZonedDateTime scheduledDateTime, Long caseId) {
        MvcResult postResponse = mockMvc
            .perform(
                post("/timed-event")
                    .content(buildTimedEvent(Event.EXAMPLE, scheduledDateTime, caseId))
                    .contentType("application/json")
            )
            .andExpect(status().isCreated())
            .andReturn();

        return objectMapper.readValue(postResponse.getResponse().getContentAsString(), TimedEvent.class);
    }

    /**
     * This splitting of one sleep into multiple ones is needed for the test as unfortunately without it the tests
     * sometimes fail as if Quartz is running on the same thread as the sleep instruction and the sleep is interfering
     * preventing the scheduled operation to happen or to be deleted.
     * @param totalMillis The total wait time
     */
    @SneakyThrows
    private void weirdSleep(long totalMillis) {
        long total = 0;

        while (total < totalMillis) {
            Thread.sleep(INCREMENT);
            total += INCREMENT;
        }

    }

    @SneakyThrows
    private String buildTimedEvent(Event event, ZonedDateTime scheduledDateTime, long caseId) {
        //scheduledDateTime.format(DateTimeFormatter.ISO_DATE_TIME)
        TimedEvent timedEvent = new TimedEvent(
            null,
            event,
            scheduledDateTime,
            "IA",
            "Asylum",
            caseId);
        return objectMapper.writeValueAsString(timedEvent);
    }

}
