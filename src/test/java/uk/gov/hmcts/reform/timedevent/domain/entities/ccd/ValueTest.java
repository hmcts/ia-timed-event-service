package uk.gov.hmcts.reform.timedevent.domain.entities.ccd;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.entities.ccd.Value;

class ValueTest {

    @Test
    void testValue() {

        Value value1 = new Value("code", "label");
        Value value2 = new Value("code", "label");

        assertEquals(value1, value2);
        assertEquals(value1.getCode(), value2.getCode());
        assertEquals(value1.getLabel(), value2.getLabel());
    }
}
