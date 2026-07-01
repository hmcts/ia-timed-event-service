package uk.gov.hmcts.reform.timedevent.infrastructure.clients.model.ccd;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Collections;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CaseDataContent {

    private Event event;
    private String eventToken;
    private boolean ignoreWarning;
    @Getter(lombok.AccessLevel.NONE)
    private Map<String, Object> data;

    public Map<String, Object> getData() {
        return data == null ? Collections.emptyMap() : Collections.unmodifiableMap(data);
    }

}
