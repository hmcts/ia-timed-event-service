package uk.gov.hmcts.reform.timedevent.infrastructure.clients.model.ccd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CaseDetails {

    private Long id;
    private String state;
    @Getter(lombok.AccessLevel.NONE)
    private Map<String, Object> caseData;

    public Map<String, Object> getCaseData() {
        return caseData == null ? Collections.emptyMap() : Collections.unmodifiableMap(caseData);
    }
}
