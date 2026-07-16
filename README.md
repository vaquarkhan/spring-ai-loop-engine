# Spring AI Loop Engine

Enterprise-grade **Loop Engineering** for [Spring AI](https://docs.spring.io/spring-ai/reference/): a zero-configuration starter that abstracts tactical prompt engineering into autonomous, stateful, and secure agent loops.

> Prompting does not disappear — it is abstracted. Developers become **loop architects** who design goals, tool circuits, and safety guardrails. The loop handles self-correction until the goal is met.

## Why this exists

Default recursive `ToolCallingAdvisor` chains lack:

- Per-turn state and runaway billing protection  
- Protocol interoperability (AG-UI / A2A / MCP)  
- Enterprise integrity (output gates + decision attestation)  
- GenAI-native observability with DLP  

`spring-ai-loop-engine` addresses those gaps while staying compatible with Spring AI’s `ChatClient`, `ToolCallback`, and auto-configuration model.

## Naming (Spring AI Community)

| Item | Value |
|------|-------|
| Repository / artifact | `spring-ai-loop-engine` |
| Starter | `spring-ai-starter-loop-engine` |
| GroupId (pre-incubation) | `io.github.vaquarkhan` |
| License | Apache 2.0 |

Community proposal: [spring-ai-community/community#28](https://github.com/spring-ai-community/community/issues/28)

## Quick start

```xml
<dependency>
  <groupId>io.github.vaquarkhan</groupId>
  <artifactId>spring-ai-starter-loop-engine</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

```yaml
spring:
  ai:
    loop:
      soft-max-rounds: 15
      hard-max-rounds: 25
      block-duplicate-failed-actions: true
```

```java
@Autowired AgentLoopManager loops;

LoopResult result = loops.run(LoopRequest.builder()
    .userMessage("Reconcile yesterday's failed invoices")
    .systemPrompt("You are a careful finance operations agent.")
    .build());
```

## Architecture

```
User / Frontend (AG-UI SSE)
        │
        ▼
 AgentLoopManager  ── soft/hard bounds, fingerprints, AgentTurn
        │
   ┌────┼──────────────┐
   ▼    ▼              ▼
 Chat  Tools        Listeners
Client (MCP Bastion)  AG-UI · Integrity/PVDM · OTel
   │
   └── A2A SubAgentSpawner + /.well-known/agent-card.json
```

### Core: `AgentLoopManager`

- Decorates Spring AI tool execution (does not replace `ChatClient`)  
- `AgentTurn` tracks rounds, tool history, argument fingerprints  
- **Soft-max-rounds** → inject wrap-up prompt, stop tools  
- **Hard-max-rounds** → `HardMaxRoundsExceededException` (stop billing)  

### Protocol Triad

| Protocol | Module | Capability |
|----------|--------|------------|
| **AG-UI** | `spring-ai-loop-engine-agui` | WebFlux SSE (`RunStartedEvent`, `TOOL_CALL_START`, `STATE_DELTA`), HITL approvals |
| **A2A** | `spring-ai-loop-engine-a2a` | Sub-agent spawning with budgets, `/.well-known/agent-card.json` |
| **MCP** | `spring-ai-loop-engine-mcp` | Zero-trust Bastion (RBAC), Cursor `mcp.json` generator |

### Integrity & Observability

- Density / dependency / YAML design **validation gates**  
- **PVDM-A** HMAC decision attestations per tool call and loop completion  
- OpenTelemetry GenAI spans (`gen_ai.usage.*`, finish reasons, spawn latency)  
- `PiiMaskingSpanExporter` for SSN / email / API key redaction  

## Modules

| Module | Description |
|--------|-------------|
| `spring-ai-loop-engine-core` | `AgentLoopManager`, `AgentTurn`, bounds, fingerprinting |
| `spring-ai-loop-engine-agui` | AG-UI SSE + HITL |
| `spring-ai-loop-engine-a2a` | Sub-agents + AgentCard |
| `spring-ai-loop-engine-mcp` | Bastion + Cursor config |
| `spring-ai-loop-engine-integrity` | Gates + PVDM |
| `spring-ai-loop-engine-observability` | OTel + PII masking |
| `spring-ai-starter-loop-engine` | One dependency for everything |
| `spring-ai-loop-engine-bom` | BOM |

## Cursor IDE

See [docs/cursor-setup.md](docs/cursor-setup.md).

Cursor project files live under `.cursor/`:

- `.cursor/rules/loop-engine.mdc` — always-on project rules
- `.cursor/mcp.json` — MCP snippet for Composer

```bash
# Unix
./scripts/install.sh --tool cursor

# Windows PowerShell
./scripts/install.ps1 -Tool cursor
```

## Example

```bash
mvn -pl examples/simple-loop-app -am spring-boot:run
curl -X POST http://localhost:8080/api/loop/run -H "Content-Type: application/json" -d "{\"message\":\"demo\"}"
```

## Stack

| Baseline | Version |
|----------|---------|
| Java | 21+ |
| Spring Boot | 3.5.x (profile `spring-ai-2` reserved for Boot 4 / Spring AI 2.0) |
| Spring AI | 1.1.x |

Reference implementation patterns: [ai-agent-java-sdk](https://github.com/vaquarkhan/ai-agent-java-sdk) (Strands-style Java port).

## License

Apache License 2.0
