# spring-ai-loop-engine-core

Core Loop Engineering runtime for Spring AI.

This module owns the stateful agent loop: turn tracking, soft/hard round budgets, tool-argument fingerprinting, and the Spring Boot auto-configuration that wires `AgentLoopManager`.

## Artifact

```xml
<dependency>
  <groupId>io.github.vaquarkhan</groupId>
  <artifactId>spring-ai-loop-engine-core</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Package layout

| Package | Contents |
|---------|----------|
| `...core.loop` | `AgentLoopManager`, `LoopRequest`, `LoopModelClient`, `ChatClientLoopModelClient`, `LoopListener` |
| `...core.model` | `AgentTurn`, `LoopResult`, `ToolCallFingerprint`, `TerminationReason` |
| `...core.tool` | `ToolExecutor`, `ToolCallbackToolExecutor`, `ArgumentFingerprinter` |
| `...core.config` | `LoopEngineProperties`, `LoopEngineAutoConfiguration` |

## Key classes

- **`AgentLoopManager`** — runs the model → tool → feedback loop with soft wrap-up and hard stop
- **`AgentTurn`** — per-user-message state (round count, tool history, failed fingerprints)
- **`LoopRequest` / `LoopResult`** — input and output of one loop run
- **`HardMaxRoundsExceededException`** — thrown when hard-max-rounds is exceeded
- **`ChatClientLoopModelClient`** — wraps Spring AI `ChatClient` without replacing it

## Configuration (`spring.ai.loop.*`)

| Property | Default | Description |
|----------|---------|-------------|
| `enabled` | `true` | Master switch |
| `soft-max-rounds` | `15` | Inject wrap-up prompt; stop further tools |
| `hard-max-rounds` | `25` | Force stop (exception) |
| `block-duplicate-failed-actions` | `true` | Block same tool+args after a failure |
| `system-prompt` | (built-in) | Default system prompt when none is supplied |

## Example

```java
LoopResult result = agentLoopManager.run(LoopRequest.builder()
    .sessionId("ops-1")
    .userMessage("Reconcile failed invoices")
    .systemPrompt("You are a careful finance agent.")
    .softMaxRounds(10)
    .hardMaxRounds(20)
    .build());
```

## Tests

```bash
mvn -pl spring-ai-loop-engine-core test
```

Covers completion, tool execution, duplicate fingerprint blocking, soft wrap-up, and hard-max stop.
