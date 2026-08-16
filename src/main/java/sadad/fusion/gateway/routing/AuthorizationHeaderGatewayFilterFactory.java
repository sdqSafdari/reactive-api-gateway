package sadad.fusion.gateway.routing;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationHeaderGatewayFilterFactory
        extends AbstractGatewayFilterFactory<AuthorizationHeaderGatewayFilterFactory.Config> {

    public AuthorizationHeaderGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new AuthorizationHeaderGatewayFilter();
    }

    public static class Config {
        // put any YAML-configurable fields here, e.g. a flag or header name
        // leave empty if you don't need per-route config
    }
}
