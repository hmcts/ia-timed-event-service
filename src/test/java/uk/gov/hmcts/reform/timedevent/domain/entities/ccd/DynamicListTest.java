package uk.gov.hmcts.reform.timedevent.domain.entities.ccd;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
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

    @Test
    void getListItems_returns_empty_list_when_null() {
        DynamicList dynamicList = new DynamicList(value, null);

        List<Value> listItems = dynamicList.getListItems();

        assertNull(listItems);
    }

    @Test
    void getListItems_returns_unmodifiable_list() {
        Value item1 = new Value("code1", "label1");
        Value item2 = new Value("code2", "label2");
        List<Value> items = Arrays.asList(item1, item2);
        DynamicList dynamicList = new DynamicList(value, items);

        List<Value> returnedItems = dynamicList.getListItems();

        assertThrows(UnsupportedOperationException.class, () -> returnedItems.add(item1));
        assertThrows(UnsupportedOperationException.class, () -> returnedItems.remove(0));
        assertThrows(UnsupportedOperationException.class, () -> returnedItems.clear());
    }

    @Test
    void getListItems_returns_correct_values() {
        Value item1 = new Value("code1", "label1");
        Value item2 = new Value("code2", "label2");
        List<Value> items = Arrays.asList(item1, item2);
        DynamicList dynamicList = new DynamicList(value, items);

        List<Value> returnedItems = dynamicList.getListItems();

        assertEquals(2, returnedItems.size());
        assertEquals("code1", returnedItems.get(0).getCode());
        assertEquals("code2", returnedItems.get(1).getCode());
    }
}
