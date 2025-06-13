package uk.gov.hmcts.reform.timedevent.testutils.data;

import feign.FeignException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.timedevent.infrastructure.clients.IdamApi;
import uk.gov.hmcts.reform.timedevent.infrastructure.clients.model.idam.Token;
import uk.gov.hmcts.reform.timedevent.infrastructure.clients.model.idam.UserInfo;
import uk.gov.hmcts.reform.timedevent.infrastructure.security.oauth2.IdentityManagerResponseException;

@Service
public class IdamAuthProvider {

    private final IdamApi idamApi;
    private final String idamRedirectUrl;
    private final String userScope;
    private final String idamClientId;
    private final String idamClientSecret;

    public IdamAuthProvider(
        IdamApi idamApi,
        String idamRedirectUrl,
        String userScope,
        String idamClientId,
        String idamClientSecret
    ) {
        this.idamApi = idamApi;
        this.idamRedirectUrl = idamRedirectUrl;
        this.userScope = userScope;
        this.idamClientId = idamClientId;
        this.idamClientSecret = idamClientSecret;
    }

    public String getUserToken(String username, String password) {

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "password");
        map.add("redirect_uri", idamRedirectUrl);
        map.add("client_id", idamClientId);
        map.add("client_secret", idamClientSecret);
        map.add("username", username);
        map.add("password", password);
        map.add("scope", userScope);
        try {
            Token tokenResponse = idamApi.token(map);
            return "Bearer " + tokenResponse.getAccessToken();
        } catch (FeignException ex) {
            throw new IdentityManagerResponseException("Could not get user token from IDAM", ex);
        }
    }

    @Cacheable(value = "legalRepATokenCache")
    public String getLegalRepToken() {
        return getUserToken(
            System.getenv("TEST_LAW_FIRM_A_USERNAME"),
            System.getenv("TEST_LAW_FIRM_A_PASSWORD")
        );
    }

    @Cacheable(value = "caseOfficerTokenCache")
    public String getCaseOfficerToken() {
        return getUserToken(
            System.getenv("TEST_CASEOFFICER_USERNAME"),
            System.getenv("TEST_CASEOFFICER_PASSWORD")
        );
    }

    @Cacheable(value = "homeOfficeLartTokenCache")
    public String getHomeOfficeLartToken() {
        return getUserToken(
            System.getenv("TEST_HOMEOFFICE_LART_USERNAME"),
            System.getenv("TEST_HOMEOFFICE_LART_PASSWORD")
        );
    }

    @Cacheable(value = "systemTokenCache")
    public String getSystemUserToken() {
        return getUserToken(
            System.getenv("IA_SYSTEM_USERNAME"),
            System.getenv("IA_SYSTEM_PASSWORD")
        );
    }

    public String getUserId(String token) {
        try {
            UserInfo userInfo = idamApi.userInfo(token);
            return userInfo.getUid();
        } catch (FeignException ex) {
            throw new IdentityManagerResponseException("Could not get system user token from IDAM", ex);
        }
    }
}
