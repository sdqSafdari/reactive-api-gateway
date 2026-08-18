package sadad.fusion.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * is automatically registered with OIDC_ReactiveAuthenticationManager
 */
@Component
public class OidcUserService implements ReactiveOAuth2UserService<OidcUserRequest, OidcUser> {
    private final OidcReactiveOAuth2UserService oidcReactiveOAuth2UserService;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public OidcUserService() {
        this.oidcReactiveOAuth2UserService = new OidcReactiveOAuth2UserService();
    }

    @Override
    public Mono<OidcUser> loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        logger.info("oauth2 token response, access_token: {}", userRequest.getAccessToken().getTokenValue());
        logger.info("oidc token response, id_token: {}", userRequest.getIdToken().getTokenValue());
        return this.oidcReactiveOAuth2UserService
                .loadUser(userRequest) // call UserInfo endpoint
                .map(oidcUser -> new OidcUserPrincipal(oidcUser, userRequest.getAccessToken()))
                ;
    }
}
