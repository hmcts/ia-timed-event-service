package uk.gov.hmcts.reform.timedevent.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.timedevent.domain.entities.ccd.Event;

class SecurityConfigurationTest {

    @SuppressWarnings("unchecked")
    private final Converter<Jwt, Collection<GrantedAuthority>> mockConverter = mock(Converter.class);
    private final ServiceAuthFilter mockServiceAuthFilter = mock(ServiceAuthFilter.class);

    @Test
    void getAnonymousPaths_returns_empty_list_when_empty() {
        SecurityConfiguration config = new SecurityConfiguration(mockConverter, mockServiceAuthFilter);

        List<String> paths = config.getAnonymousPaths();

        assertNotNull(paths);
        assertTrue(paths.isEmpty());
    }

    @Test
    void getAnonymousPaths_returns_immutable_list() {
        SecurityConfiguration config = new SecurityConfiguration(mockConverter, mockServiceAuthFilter);

        List<String> paths = config.getAnonymousPaths();

        assertThrows(UnsupportedOperationException.class, () -> paths.add("/path"));
    }

    @Test
    void getRoleEventAccess_returns_empty_map_when_empty() {
        SecurityConfiguration config = new SecurityConfiguration(mockConverter, mockServiceAuthFilter);

        Map<String, List<Event>> roleEventAccess = config.getRoleEventAccess();

        assertNotNull(roleEventAccess);
        assertTrue(roleEventAccess.isEmpty());
    }

    @Test
    void getRoleEventAccess_returns_immutable_map() {
        SecurityConfiguration config = new SecurityConfiguration(mockConverter, mockServiceAuthFilter);

        Map<String, List<Event>> roleEventAccess = config.getRoleEventAccess();

        assertThrows(UnsupportedOperationException.class, () -> roleEventAccess.put("role", List.of()));
    }
}
