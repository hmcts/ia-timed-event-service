package uk.gov.hmcts.reform.timedevent.domain.entities.roleassignment;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoleAssignmentResourceTest {

    @Test
    void roleAssignmentResponse_returns_empty_list_when_null() {
        RoleAssignmentResource resource = new RoleAssignmentResource(null);

        List<Assignment> response = resource.roleAssignmentResponse();

        assertNotNull(response);
        assertTrue(response.isEmpty());
    }

    @Test
    void roleAssignmentResponse_returns_unmodifiable_list() {
        Assignment assignment = Assignment.builder()
            .id("id")
            .actorId("actorId")
            .build();
        List<Assignment> assignments = Arrays.asList(assignment);
        RoleAssignmentResource resource = new RoleAssignmentResource(assignments);

        List<Assignment> response = resource.roleAssignmentResponse();

        assertThrows(UnsupportedOperationException.class, () -> response.add(assignment));
        assertThrows(UnsupportedOperationException.class, () -> response.remove(0));
        assertThrows(UnsupportedOperationException.class, () -> response.clear());
    }

    @Test
    void roleAssignmentResponse_returns_correct_values() {
        Assignment assignment1 = Assignment.builder().id("id1").build();
        Assignment assignment2 = Assignment.builder().id("id2").build();
        List<Assignment> assignments = Arrays.asList(assignment1, assignment2);
        RoleAssignmentResource resource = new RoleAssignmentResource(assignments);

        List<Assignment> response = resource.roleAssignmentResponse();

        assertEquals(2, response.size());
        assertEquals("id1", response.get(0).getId());
        assertEquals("id2", response.get(1).getId());
    }
}
