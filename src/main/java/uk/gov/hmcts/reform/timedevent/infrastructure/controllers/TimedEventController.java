package uk.gov.hmcts.reform.timedevent.infrastructure.controllers;

import static org.springframework.http.ResponseEntity.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.entities.TimedEvent;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.services.SchedulerService;
import uk.gov.hmcts.reform.timedevent.infrastructure.security.CcdEventAuthorizor;

@RestController
public class TimedEventController {

    private CcdEventAuthorizor ccdEventAuthorizor;
    private SchedulerService schedulerService;

    public TimedEventController(CcdEventAuthorizor ccdEventAuthorizor, SchedulerService timedEventService) {
        this.ccdEventAuthorizor = ccdEventAuthorizor;
        this.schedulerService = timedEventService;
    }

    @Operation(
        summary = "Scheduling / rescheduling timed event",
        security =
            {
                @SecurityRequirement(name = "Authorization"),
                @SecurityRequirement(name = "ServiceAuthorization")
            }
    )
    @ApiResponses({
        @ApiResponse(
            responseCode  = "201",
            description = "Created TimeEvent object",
            content =  @Content(schema = @Schema(implementation = TimedEvent.class))
            ),
        @ApiResponse(
            responseCode = "415",
            description = "Unsupported Media Type"
            ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request"
            ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden"
            ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error"
            )
    })
    @PostMapping("/timed-event")
    public ResponseEntity<TimedEvent> post(@RequestBody TimedEvent timedEvent) {

        if (!isValid(timedEvent)) {
            return badRequest().build();
        }

        ccdEventAuthorizor.throwIfNotAuthorized(timedEvent.getEvent());

        String identity = "";
        if (StringUtils.isBlank(timedEvent.getId())) {
            identity = schedulerService.schedule(timedEvent);
        } else {
            identity = schedulerService.reschedule(timedEvent, 0);
        }

        return status(HttpStatus.CREATED).body(
            new TimedEvent(
                identity,
                timedEvent.getEvent(),
                timedEvent.getScheduledDateTime(),
                timedEvent.getJurisdiction(),
                timedEvent.getCaseType(),
                timedEvent.getCaseId()
            )
        );
    }

    @DeleteMapping("/timed-event/{id}")
    public ResponseEntity<TimedEvent> delete(@PathVariable("id") String jobKey) {
        boolean result = schedulerService.deleteSchedule(jobKey);
        if (!result) {
            return status(HttpStatus.NOT_FOUND).build();
        }

        return status(HttpStatus.ACCEPTED).build();
    }

    @Operation(
        summary = "Getting scheduled event",
        security =
            {
                @SecurityRequirement(name = "Authorization"),
                @SecurityRequirement(name = "ServiceAuthorization")
            }
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "TimeEvent object",
            content =  @Content(schema = @Schema(implementation = TimedEvent.class))
            ),
        @ApiResponse(
            responseCode = "404",
            description = "Not Found"
            ),
        @ApiResponse(
            responseCode = "401",
            description = "Forbidden"
            ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error"
            )
    })
    @GetMapping("/timed-event/{id}")
    public ResponseEntity<TimedEvent> get(@PathVariable("id") String id) {

        return schedulerService
            .get(id)
            .map(ResponseEntity::ok)
            .orElse(notFound().build());
    }

    private boolean isValid(TimedEvent timedEvent) {
        return timedEvent != null
               && timedEvent.getEvent() != null
               && timedEvent.getScheduledDateTime() != null
               && timedEvent.getCaseId() != 0
               && Strings.isNotBlank(timedEvent.getJurisdiction())
               && Strings.isNotBlank(timedEvent.getCaseType());
    }
}
