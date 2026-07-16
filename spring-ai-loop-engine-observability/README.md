# spring-ai-loop-engine-observability

OpenTelemetry GenAI telemetry and PII masking for agent loops.

Standard APM is not enough for loop engineering. This module emits GenAI semantic span attributes (tokens, finish reasons, spawn latency) and redacts sensitive values before traces leave the network.

## Artifact

```xml
<dependency>
  <groupId>io.github.vaquarkhan</groupId>
  <artifactId>spring-ai-loop-engine-observability</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Package layout

| Class | Role |
|-------|------|
| `GenAiLoopTelemetryListener` | `LoopListener` that creates/ends OTel spans per turn |
| `PiiMaskingSpanExporter` | Wraps an OTLP (or any) `SpanExporter` and redacts PII |
| `ObservabilityAutoConfiguration` | Registers the telemetry listener |

## Span attributes

| Attribute | Meaning |
|-----------|---------|
| `gen_ai.operation.name` | `agent_loop` |
| `agent.turn.id` | Turn id |
| `agent.session.id` | Session id |
| `agent.rounds` | Rounds executed |
| `agent.termination_reason` | Why the loop stopped |
| `agent.spawn.latency_ms` | Elapsed turn time |
| `gen_ai.usage.input_tokens` / `output_tokens` | (constants available for metadata enrichment) |

Tool start/end events are recorded on the turn span.

## PII masking

`PiiMaskingSpanExporter.mask(String)` redacts:

- Emails → `[EMAIL_REDACTED]`
- SSNs (`###-##-####`) → `[SSN_REDACTED]`
- API-key-like tokens → `[API_KEY_REDACTED]`

Wire it around your OTLP exporter in application config when exporting to Datadog, Grafana Tempo, etc.

## Configuration

| Property | Default |
|----------|---------|
| `spring.ai.loop.observability.enabled` | `true` |
| `spring.ai.loop.observability.pii-masking-enabled` | `true` |

## Tests

```bash
mvn -pl spring-ai-loop-engine-observability test
```
