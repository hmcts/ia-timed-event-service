package uk.gov.hmcts.reform.timedevent.infrastructure.security;

import java.util.Set;

public interface AuthorizedRolesProvider {

    Set<String> getRoles();

}
