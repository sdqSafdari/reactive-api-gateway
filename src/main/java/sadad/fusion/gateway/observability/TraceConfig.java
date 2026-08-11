package sadad.fusion.gateway.observability;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

import java.util.Map;

@Configuration
public class TraceConfig {
    static {
        /**
         * ObservationThreadLocalAccessor — a ThreadLocalAccessor implementation (from io.micrometer:micrometer-observation)
         * that's auto-registered with the global ContextRegistry.
         * When Hooks.enableAutomaticContextPropagation() is active, Reactor calls this accessor's setValue()/reset()
         * around every operator boundary (onNext, onError, onComplete) as execution moves across threads.
         * setValue() effectively opens an Observation.Scope
         */
        Hooks.enableAutomaticContextPropagation();
    }

    private Tracer tracer; // span lifecycle manager, TracingObservationHandler use it
    private SpanExporter spanExporter; // export spans via Brave or OTLP
    private ObservationRegistry observationRegistry; // create Observation objects and contains ObservationHandlers

    @Bean
    public Map zipkinSpanExporter(SpanExporter spanExporter, Tracer tracer){
        //for debugging purposes
        return Map.of();
    }
    @Bean
    InitializingBean openTelemetryAppenderInitializer(OpenTelemetry openTelemetry) {
        return () -> OpenTelemetryAppender.install(openTelemetry);
    }
}
