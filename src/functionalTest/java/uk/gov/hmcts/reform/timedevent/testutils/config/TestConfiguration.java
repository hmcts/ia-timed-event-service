package uk.gov.hmcts.reform.timedevent.testutils.config;

import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfiguration {

    @Bean
    public Retryer retryer() {
        return new Retryer.Default(1000L, 1000L, 3);
    }

}