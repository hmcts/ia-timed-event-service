package uk.gov.hmcts.reform.timedevent.testutils;

import static uk.gov.hmcts.reform.timedevent.infrastructure.security.oauth2.IdamAuthoritiesConverter.REGISTRATION_ID;

import feign.Retryer;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesRegistrationAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@Configuration
public class TestConfiguration {

    // prevent ClientRegistrationRepository to call real auth centre
    @Bean
    @Primary
    ClientRegistrationRepository clientRegistrationRepository(OAuth2ClientProperties properties) {

        return new ClientRegistrationRepository() {
            @Override
            public ClientRegistration findByRegistrationId(String registrationId) {
                return OAuth2ClientPropertiesRegistrationAdapter.getClientRegistrations(properties).get(REGISTRATION_ID);
            }
        };
    }
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(1000L, 1000L, 3);
    }
}
