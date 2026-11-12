package uk.gov.hmcts.reform.timedevent.infrastructure.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.EnumFeature;

@Configuration
public class JacksonConfiguration {


    @Bean
    public JsonMapperBuilderCustomizer unknownEnumCustomizer() {
        return builder -> builder.enable(
                EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
    }
}
