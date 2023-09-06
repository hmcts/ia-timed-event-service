package uk.gov.hmcts.reform.timedevent.infrastructure.controllers;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.entities.TimedEvent;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.services.EventExecutor;
import uk.gov.hmcts.reform.timedevent.testutils.SpringBootIntegrationTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RetryLogicIntegrationTest extends SpringBootIntegrationTest {

    @MockBean
    EventExecutor eventExecutor;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @WithMockUser(authorities = {"caseworker-ia-caseofficer"})
    void testScheduledEventHasRunAfterAppropriateTime() {
        // Given: an event scheduled in the future
        scheduleEvent(Event.EXAMPLE, ZonedDateTime.now().plusSeconds(5), 1588772172174020L);

        // When: I wait for enough time to pass
        weirdSleep(6000);

        // Then: the event is executed
        verify(eventExecutor, times(1)).execute(any());
    }

    @Test
    @WithMockUser(authorities = {"caseworker-ia-caseofficer"})
    void testScheduledEventHasNotRunBeforeTime() {
        // Given: an event scheduled in the future
        scheduleEvent(Event.EXAMPLE, ZonedDateTime.now().plusSeconds(5), 1588772172174021L);

        // When: I don't wait for enough time to pass
        weirdSleep(1000);

        // Then: the event is not executed
        verify(eventExecutor, times(0)).execute(any());
    }

    @SneakyThrows
    @Test
    @WithMockUser(authorities = {"caseworker-ia-caseofficer"})
    void testRetryableExecutionFailureTriggersAnotherAttempt() {
        // Given: an event scheduled in the future that is destined to fail
        doThrow(FeignException.GatewayTimeout.class).when(eventExecutor).execute(any());

        scheduleEvent(Event.EXAMPLE, ZonedDateTime.now().plusSeconds(5), 1588772172174022L);

        // When: I wait for enough time to pass
        weirdSleep(30000);

        // Then: the event execution is attempted at least twice
        verify(eventExecutor, atLeast(2)).execute(any());
    }

    @Test
    @WithMockUser(authorities = {"caseworker-ia-caseofficer"})
    void testExecutionFailureMaximumAttemptLimitIsRespected() {
        // Given: an event scheduled in the future that is destined to fail
        doThrow(FeignException.GatewayTimeout.class).when(eventExecutor).execute(any());

        scheduleEvent(Event.EXAMPLE, ZonedDateTime.now().plusSeconds(5), 1588772172174023L);

        // When: I wait for enough time to pass
        weirdSleep(30000);

        // Then: the event execution is attempted exactly three times
        verify(eventExecutor, times(3)).execute(any());
    }

    @SneakyThrows
    private TimedEvent scheduleEvent(Event event, ZonedDateTime scheduledDateTime, Long caseId) {
        MvcResult postResponse = mockMvc
            .perform(
                post("/timed-event")
                    .content(buildTimedEvent(event, scheduledDateTime, caseId))
                    .contentType("application/json")
            )
            .andExpect(status().isCreated())
            .andReturn();

        return objectMapper.readValue(postResponse.getResponse().getContentAsString(), TimedEvent.class);
    }

    /**
     * This is needed for the test as unfortunately without it the tests sometimes fail as if Quartz is running on
     * the same thread as the sleep instruction
     * @param totalMillis The total wait time
     */
    @SneakyThrows
    private void weirdSleep(int totalMillis) {
        int total = 0;
        final int INCREMENT = 500;

        while(total < totalMillis) {
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
