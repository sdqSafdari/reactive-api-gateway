package sadad.fusion.gateway.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class TraceIdHeaderWebFilter implements WebFilter, Ordered {

    private final Tracer tracer;

    public TraceIdHeaderWebFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public int getOrder() {
        // run after the observation instrumentation, so the Observation
        // is already present in the Reactor Context by the time we read it
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return Mono.deferContextual(ctx -> {
            Observation observation = ctx.getOrDefault(ObservationThreadLocalAccessor.KEY, null);
            if (observation != null) {
                // scoped() opens the scope synchronously, runs the runnable,
                // then closes it — guaranteeing tracer.currentSpan() is
                // correctly populated for the duration of this one call
                observation.scoped(() -> {
                    Span span = tracer.currentSpan();
                    if (span != null) {
                        exchange.getResponse().getHeaders()
                                .add("X-Trace-Id", span.context().traceId());
                    }
                });
            }
            return chain.filter(exchange);
        });
    }
}
