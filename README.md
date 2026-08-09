### Spring Cloud Gateway Server WebFlux
Spring Cloud Gateway matches routes as part of the Spring WebFlux `HandlerMapping` infrastructure.    
Clients make requests to Spring Cloud Gateway. If the `Gateway Handler Mapping` determines that a request matches a route,    
it is sent to the `Gateway Web Handler`.    
This handler runs the request through a filter chain that is specific to the request.    


Configuration for Spring Cloud Gateway is driven by a **collection** of `RouteDefinitionLocator` instances.   
The following listing shows the definition of the RouteDefinitionLocator interface:
```Java
public interface RouteDefinitionLocator {
	Flux<RouteDefinition> getRouteDefinitions();
}
```
### Micrometer Meter
A Meter is the interface for collecting a set of measurements (which we individually call metrics) about your application.     
Micrometer supports a set of Meter primitives, including `Timer`, `Counter`, `Gauge`, `DistributionSummary`, `LongTaskTimer`, `FunctionCounter`, `FunctionTimer`, and `TimeGauge`.    
Different meter types result in a different number of time series metrics.    
For example, while there is a single metric that represents a Gauge,    
a Timer(summary metric type) measures both the count of timed events and the total time of all timed events.   

Meters in Micrometer are created from and held in a `MeterRegistry`.     
Each supported **monitoring system** has an implementation of MeterRegistry.    
How a registry is created varies for each implementation.    

Micrometer employs a naming convention that separates lowercase words with a . (dot) character.(e.g. *api.gateway.routed.error*)
Each MeterRegistry convert the standard name to its monitoring system convention.(e.g. Prometheus *api_gateway_routed_error*)

### Metrics
Spring Webflux register observation beans in `WebFluxObservationAutoConfiguration` class and register bean 
`DefaultServerRequestObservationConvention` that registers the **http_server_requests_seconds_count** metrics for incoming HTTP requests,    
*DefaultServerRequestObservationConvention* works with **ServerRequestObservationContext** object that is in the Exchange attributes.   

Spring Cloud Gateway registers **spring_cloud_gateway_requests_seconds_sum** metrics.     
**org.springframework.cloud.gateway.filter.GatewayMetricsFilter** register the above summary metric using micrometer Timer meter.    
set *spring.cloud.gateway.server.webflux.metrics.path-tags.enabled* property to add **path** tag to the above metrics.     

`Low cardinality values`: are for data with a small, bounded set of possible values. They are added to both metrics and traces.
`High cardinality values`: are for data with a large or unbounded set of possible values.    
They are added only to traces, logs, and other systems designed to handle high-cardinality data, but crucially, not to metrics.    

### Micrometer Tracing
Micrometer Tracing Library is the successor to the **Spring Cloud Sleuth**.    
Micrometer Tracing is a facade (like Micrometer itself, but for traces)    
it lets you write vendor-neutral tracing code, then plug in either bridge:    
- micrometer-tracing-bridge-brave:  uses Brave (Zipkin's tracer)
- micrometer-tracing-bridge-otel: uses the OpenTelemetry Java SDK

> [!Important]
> Remember to pick only one bridge. You should not have two bridges on the classpath.

You need to understand the following definitions for distributed tracing:   
- Span: The basic unit of work. For example, sending an RPC is a new span
- Trace: A set of spans forming a tree-like structure.
- Annotation/Event: Used to record the existence of an event in time.
- Tracer: A library that handles the lifecycle of a span.

On the contrary to Metrics, spans should only be pushed to a tracing backend(e.g. Zipkin, Jaeger)    

`spring-boot-starter-opentelemetry` library is the best way to instrument Spring-boot app. here is how:    
it registers OtlpMeterRegistry bean for Meter registration.    
> io.micrometer:micrometer-registry-otlp, is already included in the spring-boot-starter-opentelemetry.    
> With that dependency in place, Micrometer exports metrics in OTLP format to the backend at http://localhost:4318/v1/metrics.    
> To customize the location to which metrics are exported, set the management.otlp.metrics.export.url

it also brings micrometer-tracing-bridge-otel to manage span lifecycles with opentelemetry API and export it using OTLP   
> The Spring projects use the Micrometer Observation API to create observations.     
> An observation is an interesting concept in Micrometer because     
> it can be translated into a metric and a trace.
> To enable it in your application, you have to set the management.opentelemetry.tracing.export.otlp.endpoint property.

The Micrometer Observation object(from io.micrometer.observation) is Micrometer's unified instrumentation API,   
introduced to replace instrumenting metrics and traces separately.     
The idea: you instrument your code once by wrapping it in an Observation, and that single instrumentation point can simultaneously produce:    
- A metric (via a Timer/LongTaskTimer — e.g. http.server.requests)
- A trace span (via the tracing bridge — e.g. what shows up in Zipkin)
- Log correlation (MDC population)
- Anything else a custom ObservationHandler wants to do with it (custom logging, security context propagation, etc.)

How MicroMeter works:   
1. WebFilter (or ServerHttpObservationFilter/WebHttpHandlerBuilder instrumentation) creates an Observation via ObservationRegistry, and calls .start()
2. start() fans out to every registered ObservationHandler whose supportsContext() matches — including DefaultTracingObservationHandler,    
   which creates a real Span via the Tracer bean and stashes it in the Observation.Context
3. The Observation (or a reference sufficient to reconstruct "current") gets placed into Reactor Context for the reactive chain to carry across thread hops
   On each signal (via .tap() or the automatic propagation hook), the current-observation pointer is restored into ThreadLocal on whichever thread is executing, openScope() fires, onScopeOpened() runs on every matching handler, and MDC gets populated for that thread, for that signal
4. When the chain completes, stop() fires, fanning out onStop() to every handler — ending the span, recording the timer/metric, and so on


**TracingContext** object is also in Observation Object. and it includes traceId and spanId and parentSpanId.    
Spring Webflux put Observation Object in reactor Context(from `reactor.util.context.Context`)    
```
Mono.deferContextual(Mono::just)
                    .cast(Context.class)
                    .filter(context -> {
                        return context.hasKey(ObservationThreadLocalAccessor.KEY);
                        })
                    .map(context -> {
                        Observation observation = context.get(ObservationThreadLocalAccessor.KEY);
                        try (Observation.Scope scope = observation.openScope()){
                            logger.info("consider this log");
                            return scope;
                        }
                    })
```

> Spring Boot also supports sending logs via OTLP to an OpenTelemetry-capable backend,   
> but it doesn't install log appenders into Logback and Log4j2 out of the box.   
> This may change in the future   

**context propagation**:    
With context propagation, signals (traces, metrics, and logs) can be correlated with each other.     
Propagation is the mechanism that moves context between services and processes.    
Propagation is usually handled by *instrumentation libraries* and is transparent to the user.    
**Example**:    
The context (here: Trace ID and Span ID as “Parent ID”) is propagated using     
the `traceparent` header as it is defined in the W3C TraceContext specification.

```
traceparent: <version>-<trace-id>-<parent-id>-<trace-flags>
#e.g.
traceparent: 00-a0892f3577b34da6a3ce929d0e0e4736-f03067aa0ba902b7-01
```

### References
- [Spring Cloud Gateway doc](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/)
- [Micrometer Meter](https://docs.micrometer.io/micrometer/reference/concepts/meters.html)
- [Micrometer Tracing](https://docs.micrometer.io/tracing/reference/glossary.html)
- [Spring blog opentelemetry](https://spring.io/blog/2025/11/18/opentelemetry-with-spring-boot)