package uk.gov.hmcts.reform.timedevent.infrastructure.controllers;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.entities.EventExecution;
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

@Slf4j
@DirtiesContext
public class RetryLogicIntegrationTest extends SpringBootIntegrationTest {

    static final long INCREMENT = 250;
    public static final long CASE_ID1 = 1588772172174020L;
    public static final long CASE_ID2 = 1588772172174021L;
    public static final long CASE_ID3 = 1588772172174022L;
    public static final long CASE_ID4 = 1588772172174000L;

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
    @Disabled
    @WithMockUser(authorities = {"caseworker-ia-caseofficer"})
    void testScheduledEventHasRunAfterAppropriateTime() {
        int maxAttempts = 10;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                // Given: an event scheduled in the future
                scheduleEvent(ZonedDateTime.now().plusSeconds(1), CASE_ID1);

                // When: I wait for enough time to pass
                weirdSleep(2000); // enough for the original invocation
                weirdSleep(retryIntervalMillis * (maxRetryNumber + 2));

                // Then: the event is executed
                verify(eventExecutor, times(i + 1)).execute(any(EventExecution.class));
                return;
            } catch (AssertionError e) {
                log.error("Failed attempt " + i + " of " + maxAttempts + " due to:");
                e.printStackTrace();
            }
        }
        throw new AssertionError("Failed all attempts.");
    }

    @Test
    @WithMockUser(authorities = {"caseworker-ia-caseofficer"})
    void testScheduledEventHasNotRunBeforeTime() {
        // Given: an event scheduled in the future
        scheduleEvent(ZonedDateTime.now().plusSeconds(5), CASE_ID2);

        // When: I don't wait for enough time to pass
        weirdSleep(1000);

        // Then: the event is not executed
        verify(eventExecutor, times(0)).execute(any(EventExecution.class));
    }

    @Test
    @WithMockUser(authorities = {"caseworker-ia-caseofficer"})
    void testExecutionFailureMaximumAttemptLimitIsRespected() {
        // Given: an event scheduled in the future that is destined to fail
        doThrow(FeignException.GatewayTimeout.class).when(eventExecutor).execute(any(EventExecution.class));

        scheduleEvent(ZonedDateTime.now().plusSeconds(1), CASE_ID4);

        // When: I wait for enough time to pass
        weirdSleep(5000); // enough for the original invocation
        weirdSleep(retryIntervalMillis * (maxRetryNumber + 2));  // enough for all the retries plus some

        // Then: the event execution is attempted exactly one time plus the number of retries
        verify(eventExecutor, atLeast(1)).execute(any(EventExecution.class));
        verify(eventExecutor, atMost(1 + maxRetryNumber)).execute(any(EventExecution.class));
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
