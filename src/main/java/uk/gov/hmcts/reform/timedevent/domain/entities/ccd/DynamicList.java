package uk.gov.hmcts.reform.timedevent.domain.entities.ccd;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import java.util.Collections;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Getter
public class DynamicList {

    private Value value;
    @Getter(lombok.AccessLevel.NONE)
    private List<Value> listItems;

    public DynamicList(String value) {
        this.value = new Value(value, value);
    }

    private DynamicList() {
    }

    public DynamicList(Value value, List<Value> listItems) {
        this.value = value;
        this.listItems = listItems;
    }

    public List<Value> getListItems() {
        return listItems == null ? null : Collections.unmodifiableList(listItems);
    }
}
