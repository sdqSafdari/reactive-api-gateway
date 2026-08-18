package sadad.fusion.gateway.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import sadad.fusion.gateway.security.OidcUserPrincipal;

public class AuthorizationHeaderGatewayFilter implements GatewayFilter {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .mapNotNull(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .flatMap(authentication -> Mono.justOrEmpty(authentication.getPrincipal())
                        .ofType(OidcUserPrincipal.class))
                .map(oidcUser -> {
                    String token = oidcUser.getAccessToken().getTokenValue();
                    return exchange.mutate()
                            .request(builder -> builder.headers(headers -> headers.setBearerAuth(token)))
                            .build();
                })
                .defaultIfEmpty(exchange)   // real object, not Mono<Void> — this is safe to switch on
                .flatMap(chain::filter);   // chain.filter called exactly once, always
    }
}
