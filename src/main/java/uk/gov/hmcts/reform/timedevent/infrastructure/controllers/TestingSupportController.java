package uk.gov.hmcts.reform.timedevent.infrastructure.controllers;

import static org.springframework.http.ResponseEntity.*;

import feign.FeignException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.entities.EventExecution;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.entities.ccd.Event;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.entities.ccd.EventNotFoundException;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.services.EventExecutor;
import uk.gov.hmcts.reform.timedevent.infrastructure.security.SystemTokenGenerator;
import uk.gov.hmcts.reform.timedevent.infrastructure.security.SystemUserProvider;

@Slf4j
@RestController
@Profile({"test", "integration"})
public class TestingSupportController {

    private final SystemTokenGenerator systemTokenGenerator;

    private final SystemUserProvider systemUserProvider;

    private final EventExecutor eventExecutor;

    public TestingSupportController(
        SystemTokenGenerator systemTokenGenerator,
        SystemUserProvider systemUserProvider,
        EventExecutor eventExecutor) {

        this.systemTokenGenerator = systemTokenGenerator;
        this.systemUserProvider = systemUserProvider;
        this.eventExecutor = eventExecutor;
    }

    @Operation(summary = "Generating system user token")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Generated system user token",
            content =  @Content(schema = @Schema(implementation = String.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error"
        )
    })
    @GetMapping("/testing-support/token")
    public ResponseEntity<String> token() {

        return ok(systemTokenGenerator.generate());
    }

    @Operation(summary = "Getting system user id")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "system user id",
            content =  @Content(schema = @Schema(implementation = String.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error"
        )
    })
    @GetMapping("/testing-support/system-user")
    public ResponseEntity<String> systemUser() {

        return ok(systemUserProvider.getSystemUserId("Bearer " + systemTokenGenerator.generate()));
    }

    @Operation(summary = "Executing event in CCD")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "confirmation message",
            content =  @Content(schema = @Schema(implementation = String.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden / re-thrown from dependent service"
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Unprocessable Entity / re-thrown from dependent service"
        ),

        @ApiResponse(
            responseCode = "400",
            description = "Bad Request / re-thrown from dependent service"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Not Found / re-thrown from dependent service"
        ),
        @ApiResponse(
            responseCode = "504",
            description = "Gateway Timeout / re-thrown from dependent service"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error"
        )
    })
    @PostMapping("/testing-support/execute/jurisdiction/{jurisdiction}/case-type/{caseType}/cid/{cid}/event/{event}")
    public ResponseEntity<String> execute(
        @PathVariable("jurisdiction") String jurisdiction,
        @PathVariable("caseType") String caseType,
        @PathVariable("cid") long id,
        @PathVariable("event") String event
    ) {

        try {

            eventExecutor.execute(
                new EventExecution(
                    Event.fromString(event),
                    jurisdiction,
                    caseType,
                    id
                )
            );

            return ok("event: " + event + ", executed for id: " + id);

        } catch (EventNotFoundException e) {
            log.error(e.getMessage(), e);
            return badRequest().body(e.getMessage());

        } catch (FeignException e) {
            log.error(e.getMessage(), e);
            return status(e.status()).body(e.contentUTF8());

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);

        }

    }
}
