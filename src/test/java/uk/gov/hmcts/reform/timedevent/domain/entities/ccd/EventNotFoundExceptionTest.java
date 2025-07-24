package uk.gov.hmcts.reform.timedevent.domain.entities.ccd;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EventNotFoundExceptionTest {

    @Test
    public void should_return_correct_message() {
        String message = "some Exception message";
        assertEquals(message, new EventNotFoundException(message).getMessage());
    }

}
