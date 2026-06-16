package uk.gov.hmcts.reform.timedevent.infrastructure.clients.model.idam;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserInfoTest {

    @Test
    void getRoles_returns_empty_list_when_null() {
        UserInfo userInfo = new UserInfo("email", "uid", null, "name", "given", "family");

        List<String> roles = userInfo.getRoles();

        assertNotNull(roles);
        assertTrue(roles.isEmpty());
    }

    @Test
    void getRoles_returns_unmodifiable_list() {
        List<String> roles = Arrays.asList("role1", "role2");
        UserInfo userInfo = new UserInfo("email", "uid", roles, "name", "given", "family");

        List<String> returnedRoles = userInfo.getRoles();

        assertThrows(UnsupportedOperationException.class, () -> returnedRoles.add("role3"));
        assertThrows(UnsupportedOperationException.class, () -> returnedRoles.remove(0));
        assertThrows(UnsupportedOperationException.class, () -> returnedRoles.clear());
    }

    @Test
    void getRoles_returns_correct_values() {
        List<String> roles = Arrays.asList("role1", "role2");
        UserInfo userInfo = new UserInfo("email", "uid", roles, "name", "given", "family");

        List<String> returnedRoles = userInfo.getRoles();

        assertEquals(2, returnedRoles.size());
        assertEquals("role1", returnedRoles.get(0));
        assertEquals("role2", returnedRoles.get(1));
    }
}
