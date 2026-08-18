package sadad.fusion.gateway.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;

public class OidcUserPrincipal extends DefaultOidcUser {
    private final OAuth2AccessToken accessToken;

    public OidcUserPrincipal(OidcUser oidcUser,
                             OAuth2AccessToken oAuth2AccessToken) {
        super(oidcUser.getAuthorities(), oidcUser.getIdToken(), oidcUser.getUserInfo(), oidcUser.getName());
        this.accessToken = oAuth2AccessToken;
    }

    public OAuth2AccessToken getAccessToken() {
        return accessToken;
    }
}
