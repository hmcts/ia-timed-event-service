package uk.gov.hmcts.reform.timedevent.domain.entities.ccd;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DynamicListTest {

    @Mock private Value value;

    @Test
    void testValue() {

        DynamicList dynamicList1 = new DynamicList("value");
        DynamicList dynamicList2 = new DynamicList("value");

        assertEquals(dynamicList1, dynamicList2);
        assertEquals(dynamicList1.getValue(), dynamicList2.getValue());
    }

    @Test
    void testValueAndListItems() {

        List<Value> items1 = new ArrayList<>();
        List<Value> items2 = new ArrayList<>();

        DynamicList dynamicList1 = new DynamicList(value, items1);
        DynamicList dynamicList2 = new DynamicList(value, items2);

        assertEquals(dynamicList1, dynamicList2);
        assertEquals(dynamicList1.getValue(), dynamicList2.getValue());
        assertEquals(dynamicList1.getListItems(), dynamicList2.getListItems());
    }
}
