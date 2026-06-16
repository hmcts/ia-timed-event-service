package uk.gov.hmcts.reform.timedevent.domain.entities.roleassignment;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AssignmentTest {

    @Test
    void has_correct_values() {

        LocalDateTime now = LocalDateTime.now();

        Assignment assignment = new Assignment(
            "id",
            now,
            Collections.emptyList(),
            ActorIdType.IDAM,
            "actorId",
            RoleType.CASE,
            RoleName.TRIBUNAL_CASEWORKER,
            RoleCategory.JUDICIAL,
            Classification.PRIVATE,
            GrantType.BASIC,
            true,
            Collections.emptyMap()
        );

        assertEquals(
            "Assignment(id=id, "
            + "created=" + now + ","
            + " authorisations=[],"
            + " actorIdType=IDAM,"
            + " actorId=actorId,"
            + " roleType=CASE,"
            + " roleName=tribunal-caseworker,"
            + " roleCategory=JUDICIAL,"
            + " classification=PRIVATE,"
            + " grantType=BASIC,"
            + " readOnly=true,"
            + " attributes={}"
            + ")",
            assignment.toString()
        );

        assertEquals("id", assignment.getId());
        assertEquals(now, assignment.getCreated());
        assertEquals(Collections.emptyList(), assignment.getAuthorisations());
        assertEquals(ActorIdType.IDAM, assignment.getActorIdType());
        assertEquals("actorId", assignment.getActorId());
        assertEquals(RoleType.CASE, assignment.getRoleType());
        assertEquals(RoleName.TRIBUNAL_CASEWORKER, assignment.getRoleName());
        assertEquals(RoleCategory.JUDICIAL, assignment.getRoleCategory());
        assertEquals(Classification.PRIVATE, assignment.getClassification());
        assertEquals(GrantType.BASIC, assignment.getGrantType());
        assertEquals(Collections.<String, String>emptyMap(), assignment.getAttributes());
    }

    @Test
    void getAuthorisations_returns_empty_list_when_null() {
        Assignment assignment = new Assignment(
            "id", null, null, null, null, null, null, null, null, null, null, null
        );

        List<String> authorisations = assignment.getAuthorisations();

        assertNotNull(authorisations);
        assertTrue(authorisations.isEmpty());
    }

    @Test
    void getAuthorisations_returns_unmodifiable_list() {
        List<String> authorisations = Arrays.asList("auth1", "auth2");
        Assignment assignment = new Assignment(
            "id", null, authorisations, null, null, null, null, null, null, null, null, null
        );

        List<String> returnedAuthorisations = assignment.getAuthorisations();

        assertThrows(UnsupportedOperationException.class, () -> returnedAuthorisations.add("auth3"));
        assertThrows(UnsupportedOperationException.class, () -> returnedAuthorisations.remove(0));
        assertThrows(UnsupportedOperationException.class, () -> returnedAuthorisations.clear());
    }

    @Test
    void getAttributes_returns_empty_map_when_null() {
        Assignment assignment = new Assignment(
            "id", null, null, null, null, null, null, null, null, null, null, null
        );

        Map<String, String> attributes = assignment.getAttributes();

        assertNotNull(attributes);
        assertTrue(attributes.isEmpty());
    }

    @Test
    void getAttributes_returns_unmodifiable_map() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("key1", "value1");
        Assignment assignment = new Assignment(
            "id", null, null, null, null, null, null, null, null, null, null, attributes
        );

        Map<String, String> returnedAttributes = assignment.getAttributes();

        assertThrows(UnsupportedOperationException.class, () -> returnedAttributes.put("key2", "value2"));
        assertThrows(UnsupportedOperationException.class, () -> returnedAttributes.remove("key1"));
        assertThrows(UnsupportedOperationException.class, () -> returnedAttributes.clear());
    }
}
