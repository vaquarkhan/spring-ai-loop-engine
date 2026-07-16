# spring-ai-loop-engine-a2a

Agent-to-Agent (A2A) support: discoverability via AgentCard and budgeted sub-agent spawning.

A single LLM loop degrades on complex work. This module lets the orchestrator spawn temporary worker loops with strict soft/hard round budgets, and exposes `/.well-known/agent-card.json` for distributed discovery. Designed to compose with `spring-ai-a2a` community patterns.

## Artifact

```xml
<dependency>
  <groupId>io.github.vaquarkhan</groupId>
  <artifactId>spring-ai-loop-engine-a2a</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Package layout

| Class | Role |
|-------|------|
| `SubAgentSpawner` | Spawns a worker loop with per-request soft/hard bounds |
| `AgentCard` | Discovery document model |
| `AgentCardController` | Serves `/.well-known/agent-card.json` |
| `A2aAutoConfiguration` | Spring Boot wiring |

## Key APIs

```java
LoopResult worker = subAgentSpawner.spawn(
    "Summarize the Q2 ledger anomalies",
    "You are a focused worker sub-agent. Complete the goal and stop.",
    5,   // soft max rounds
    8    // hard max rounds
);
```

## AgentCard endpoint

```bash
curl http://localhost:8080/.well-known/agent-card.json
```

Returns name, description, capabilities, skills, and loop budget metadata.

## Configuration

| Property | Default |
|----------|---------|
| `spring.ai.loop.a2a.enabled` | `true` |
| `spring.ai.loop.a2a.agent-card-path` | `/.well-known/agent-card.json` |
| `spring.ai.loop.a2a.agent-name` | `spring-ai-loop-engine` |
| `spring.ai.loop.a2a.agent-description` | Stateful Spring AI agent loop… |
| `spring.ai.loop.a2a.default-sub-agent-token-budget` | `8000` |

## Notes

- Sub-agent budgets are applied via `LoopRequest` overrides (thread-safe; does not mutate global properties).
- Prefer composing with the incubated `spring-ai-a2a` project for full A2A server protocol coverage.
