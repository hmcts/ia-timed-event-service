package uk.gov.hmcts.reform.timedevent.infrastructure.clients.model.ccd;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CaseDetailsTest {

    @Test
    void getCaseData_returns_empty_map_when_null() {
        CaseDetails caseDetails = new CaseDetails(1L, "state", null);

        Map<String, Object> caseData = caseDetails.getCaseData();

        assertNotNull(caseData);
        assertTrue(caseData.isEmpty());
    }

    @Test
    void getCaseData_returns_unmodifiable_map() {
        Map<String, Object> data = new HashMap<>();
        data.put("key1", "value1");
        CaseDetails caseDetails = new CaseDetails(1L, "state", data);

        Map<String, Object> returnedData = caseDetails.getCaseData();

        assertThrows(UnsupportedOperationException.class, () -> returnedData.put("key2", "value2"));
        assertThrows(UnsupportedOperationException.class, () -> returnedData.remove("key1"));
        assertThrows(UnsupportedOperationException.class, () -> returnedData.clear());
    }

    @Test
    void getCaseData_returns_correct_values() {
        Map<String, Object> data = new HashMap<>();
        data.put("key1", "value1");
        data.put("key2", "value2");
        CaseDetails caseDetails = new CaseDetails(1L, "state", data);

        Map<String, Object> returnedData = caseDetails.getCaseData();

        assertEquals(2, returnedData.size());
        assertEquals("value1", returnedData.get("key1"));
        assertEquals("value2", returnedData.get("key2"));
    }
}
