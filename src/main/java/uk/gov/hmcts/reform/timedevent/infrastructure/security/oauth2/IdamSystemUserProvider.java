package uk.gov.hmcts.reform.timedevent.infrastructure.security.oauth2;

import feign.FeignException;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.timedevent.infrastructure.clients.IdamApi;
import uk.gov.hmcts.reform.timedevent.infrastructure.domain.services.IdamService;
import uk.gov.hmcts.reform.timedevent.infrastructure.security.SystemUserProvider;

@Component
public class IdamSystemUserProvider implements SystemUserProvider {

    private final IdamApi idamApi;
    private final IdamService idamService;

    public IdamSystemUserProvider(IdamApi idamApi,
        IdamService idamService) {
        this.idamApi = idamApi;
        this.idamService = idamService;
    }

    @Override
    public String getSystemUserId(String userToken) {

        try {

            return idamService.getUserInfo(userToken).getUid();

        } catch (FeignException ex) {

            throw new IdentityManagerResponseException("Could not get system user id from IDAM", ex);
        }

    }
}
