package uk.gov.hmcts.reform.timedevent.infrastructure.clients.model.ccd;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CaseDataContentTest {

    @Test
    void getData_returns_empty_map_when_null() {
        CaseDataContent caseDataContent = new CaseDataContent(null, "token", false, null);

        Map<String, Object> data = caseDataContent.getData();

        assertNotNull(data);
        assertTrue(data.isEmpty());
    }

    @Test
    void getData_returns_unmodifiable_map() {
        Map<String, Object> data = new HashMap<>();
        data.put("key1", "value1");
        CaseDataContent caseDataContent = new CaseDataContent(null, "token", false, data);

        Map<String, Object> returnedData = caseDataContent.getData();

        assertThrows(UnsupportedOperationException.class, () -> returnedData.put("key2", "value2"));
        assertThrows(UnsupportedOperationException.class, () -> returnedData.remove("key1"));
        assertThrows(UnsupportedOperationException.class, () -> returnedData.clear());
    }

    @Test
    void getData_returns_correct_values() {
        Map<String, Object> data = new HashMap<>();
        data.put("key1", "value1");
        data.put("key2", "value2");
        CaseDataContent caseDataContent = new CaseDataContent(null, "token", false, data);

        Map<String, Object> returnedData = caseDataContent.getData();

        assertEquals(2, returnedData.size());
        assertEquals("value1", returnedData.get("key1"));
        assertEquals("value2", returnedData.get("key2"));
    }
}
