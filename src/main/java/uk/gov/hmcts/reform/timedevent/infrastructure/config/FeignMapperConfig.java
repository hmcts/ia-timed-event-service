package uk.gov.hmcts.reform.timedevent.infrastructure.config;

import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;

public final class FeignMapperConfig {

    public static final JsonMapper INSTANCE = JsonMapper.builder()
            .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            .build();

    private FeignMapperConfig() {
    }
}