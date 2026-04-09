package uk.gov.hmcts.reform.timedevent.infrastructure.controllers;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import uk.gov.hmcts.reform.timedevent.domain.entities.EventExecution;
import uk.gov.hmcts.reform.timedevent.domain.entities.TimedEvent;
import uk.gov.hmcts.reform.timedevent.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.timedevent.domain.services.EventExecutor;
import uk.gov.hmcts.reform.timedevent.testutils.SpringBootIntegrationTest;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.timedevent.testutils.Utils.retryTestCodeBlock;

@Slf4j
@DirtiesContext
public class RetryLogicIntegrationTest extends SpringBootIntegrationTest {

    static final long INCREMENT = 250;
    public static final long CASE_ID1 = 1588772172174020L;
    public static final long CASE_ID2 = 1588772172174021L;
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
        weirdSleep((int) (retryIntervalMillis * 1.5f));
    }

    @Test
    @WithMockUser(authorities = {"tribunal-caseworker"})
    void testScheduledEventHasNotRunBeforeTime() {
        // Given: an event scheduled in the future
        scheduleEvent(ZonedDateTime.now().plusSeconds(5), CASE_ID2);

        // When: I don't wait for enough time to pass
        weirdSleep(1000);

        // Then: the event is not executed
        verify(eventExecutor, times(0)).execute(any(EventExecution.class));
    }

    @Test
    @WithMockUser(authorities = {"tribunal-caseworker"})
    void testScheduledEventHasRunAfterAppropriateTime() {
        retryTestCodeBlock(10, () -> {
            // Given: an event scheduled in the near future
            scheduleEvent(ZonedDateTime.now().plusSeconds(5), CASE_ID1);

            // When: I wait enough time to pass
            await()
                .atMost(2000 + (retryIntervalMillis * (maxRetryNumber + 2)), TimeUnit.MILLISECONDS)
                .untilAsserted(() -> verify(eventExecutor, times(1)).execute(any(EventExecution.class)));
        });
    }

    @Test
    @WithMockUser(authorities = {"tribunal-caseworker"})
    void testExecutionFailureMaximumAttemptLimitIsRespected() {
        retryTestCodeBlock(10, () -> {
            doThrow(FeignException.GatewayTimeout.class).when(eventExecutor).execute(any(EventExecution.class));

            scheduleEvent(ZonedDateTime.now(ZoneId.systemDefault()).plusSeconds(5), CASE_ID4);

            await()
                .atMost((retryIntervalMillis * (maxRetryNumber + 2) * 2), TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    verify(eventExecutor, atLeast(1)).execute(any(EventExecution.class));
                    verify(eventExecutor, atMost(1 + maxRetryNumber)).execute(any(EventExecution.class));
                });
        });
    }

    @SneakyThrows
    private void scheduleEvent(ZonedDateTime scheduledDateTime, Long caseId) {
        mockMvc
            .perform(
                post("/timed-event")
                    .content(buildTimedEvent(scheduledDateTime, caseId))
                    .contentType("application/json")
                    .header("Authorization", "userToken")
                    .header("ServiceAuthorization", "s2sToken")
            )
            .andExpect(status().isCreated())
            .andReturn();
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
    private String buildTimedEvent(ZonedDateTime scheduledDateTime, long caseId) {
        //scheduledDateTime.format(DateTimeFormatter.ISO_DATE_TIME)
        TimedEvent timedEvent = new TimedEvent(
            null,
            Event.EXAMPLE,
            scheduledDateTime,
            "IA",
            "Asylum",
            caseId);
        return objectMapper.writeValueAsString(timedEvent);
    }

}
