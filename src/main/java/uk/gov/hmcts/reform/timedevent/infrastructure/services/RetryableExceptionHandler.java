package uk.gov.hmcts.reform.timedevent.infrastructure.services;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.timedevent.infrastructure.services.exceptions.NonRetryableException;
import uk.gov.hmcts.reform.timedevent.infrastructure.services.exceptions.RetryableException;

@Slf4j
@Component
public class RetryableExceptionHandler {

    public void wrapException(Exception ex) throws RetryableException, NonRetryableException {

        if (isExceptionNonRetryable(ex)) {

            log.error(ex.getMessage(), ex);
            throw new NonRetryableException();
        }

        log.info(ex.getMessage(), ex);
        // throw RetryableException by default - it is safe because we have limited number of retries
        throw new RetryableException();
    }

    private boolean isExceptionNonRetryable(Exception ex) {

        if (ex instanceof FeignException e) {

            return caseStatusDidNotQualify(e) || caseStatusDoesNotExist(e);
        }

        return false;
    }

    private boolean caseStatusDidNotQualify(FeignException e) {

        return e.status() == 422
               && e.contentUTF8().contains("The case status did not qualify for the event");
    }

    private boolean caseStatusDoesNotExist(FeignException e) {

        return e.status() == 400
               && e.contentUTF8().contains("Case reference is not valid");
    }
}
