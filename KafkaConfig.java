No — confirmed. The sol_event_* counters in SyslogMetricsTransformer are lazily created via meterRegistry.counter(name, tags) inside transform(), called only when a matching syslog event arrives. Nothing at startup (no @PostConstruct, MeterBinder, or MeterRegistryCustomizer) pre-registers them.
Evidence:
initializeCounterNames() (static block) only populates a Map<String,String> of names, not actual Counter/Meter objects — no registry interaction.
Actual meter creation/increment happens only inside transform(Message<?> message).
Tests confirm this: SyslogMetricsTransformerTest and OutboundFlowKafkaConfigTest.testMetricsIntegrationFlow() both call transform()/send a message first, then look up the counter and assert it now exists with count 1.0 — implying it didn't exist before.
Practical effect on /actuator/prometheus:
Built-in Spring Boot/Micrometer/JVM/HTTP metrics: exposed immediately at startup.
Custom sol_event_* counters: absent from the scrape output until the corresponding syslog event type has occurred at least once. This means Prometheus/Grafana dashboards or alerts based on absent() or rate-over-zero-baseline queries won't see these series until first occurrence — a common gotcha for alerting on "count of X == 0 for too long" style rules.
If you'd like, I can implement eager pre-registration (e.g., iterate all known event/counter names at startup and call meterRegistry.counter(name, tags).increment(0) or use Counter.builder(...).register(...) for each without incrementing) so all sol_event_* series show up at zero from the first scrape. Want me to add that?
