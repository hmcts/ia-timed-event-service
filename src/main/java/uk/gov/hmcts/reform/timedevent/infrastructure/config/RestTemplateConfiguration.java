package uk.gov.hmcts.reform.timedevent.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

@Configuration
@Slf4j
@SuppressWarnings("removal")
public class RestTemplateConfiguration {

    @Bean
    public RestOperations restOperations(
            ObjectMapper objectMapper
    ) {
        return restTemplate(objectMapper);
    }

    @Bean
    public RestTemplate restTemplate(ObjectMapper objectMapper) {
        RestTemplate restTemplate = new RestTemplate();

        restTemplate.getMessageConverters()
                .forEach(c -> log.info("BEFORE converter: {}", c.getClass().getName()));

        restTemplate.getMessageConverters().removeIf(converter ->
                converter.getClass().getName().startsWith("org.springframework.http.converter.json.")
                        && converter.getClass().getSimpleName().contains("Jackson")
        );

        restTemplate.getMessageConverters().addFirst(
                new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper)
        );

        log.info("AFTER");
        log.info("modules: {}, inclusion: {}",
                objectMapper.getRegisteredModuleIds(),
                objectMapper.getSerializationConfig().getDefaultPropertyInclusion());

        restTemplate.getMessageConverters()
                .forEach(c -> log.info("AFTER converter: {}", c.getClass().getName()));

        return restTemplate;
    }

}
