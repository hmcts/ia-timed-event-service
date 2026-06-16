package uk.gov.hmcts.reform.timedevent.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class AuthCheckerConfigurationTest {

    @Test
    void getAuthorisedServices_returns_empty_list_when_empty() {
        AuthCheckerConfiguration config = new AuthCheckerConfiguration();

        List<String> services = config.getAuthorisedServices();

        assertNotNull(services);
        assertTrue(services.isEmpty());
    }

    @Test
    void getAuthorisedServices_returns_immutable_list() {
        AuthCheckerConfiguration config = new AuthCheckerConfiguration();

        List<String> services = config.getAuthorisedServices();

        assertThrows(UnsupportedOperationException.class, () -> services.add("service"));
    }
}
