# spring-ai-starter-loop-engine

Zero-configuration Spring Boot starter for the Loop Engine.

Add this single dependency to pull in core, AG-UI, A2A, MCP Bastion, integrity, and observability with auto-configuration enabled by default.

## Artifact

```xml
<dependency>
  <groupId>io.github.vaquarkhan</groupId>
  <artifactId>spring-ai-starter-loop-engine</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## What it includes

| Transitive module | Role |
|-------------------|------|
| `spring-ai-loop-engine-core` | `AgentLoopManager` |
| `spring-ai-loop-engine-agui` | AG-UI SSE + HITL |
| `spring-ai-loop-engine-a2a` | Sub-agents + AgentCard |
| `spring-ai-loop-engine-mcp` | Bastion + mcp.json generator |
| `spring-ai-loop-engine-integrity` | Gates + PVDM |
| `spring-ai-loop-engine-observability` | OTel + PII masking |

## Minimal application.yml

```yaml
spring:
  ai:
    loop:
      soft-max-rounds: 15
      hard-max-rounds: 25
      agui:
        enabled: true
      a2a:
        enabled: true
      mcp:
        bastion-enabled: true
      integrity:
        enabled: true
      observability:
        enabled: true
```

## Usage

```java
@Autowired
AgentLoopManager loops;

LoopResult result = loops.run(LoopRequest.builder()
    .userMessage("Complete the reconciliation")
    .build());
```

Disable individual features with `spring.ai.loop.<feature>.enabled=false` as needed.

See the [root README](../README.md) for architecture and the per-module READMEs for details.
