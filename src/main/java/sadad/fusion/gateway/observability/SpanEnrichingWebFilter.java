package sadad.fusion.gateway.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class SpanEnrichingWebFilter implements WebFilter, Ordered {

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE; // run late, well after Security has authenticated
    }

    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, WebFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .mapNotNull(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .flatMap(auth -> Mono.deferContextual(ctx -> {
                    Observation observation = ctx.getOrDefault(ObservationThreadLocalAccessor.KEY, null);
                    if (observation != null) {
                        observation.highCardinalityKeyValue("user.id", auth.getName());
                    }
                    return chain.filter(exchange);
                }))
                .switchIfEmpty(chain.filter(exchange)); // unauthenticated requests just proceed untagged
    }
}
