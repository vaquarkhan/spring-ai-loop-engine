# Spring AI Loop Engine

## Introduction

The AI industry is rapidly shifting from manual prompt engineering to **Loop Engineering**. Instead of developers manually guiding an LLM turn-by-turn, modern AI architectures rely on autonomous loops: a developer defines a high-level goal, equips the agent with tools, and the system autonomously executes, self-corrects, and iterates until the objective is achieved.

The **Spring AI Loop Engine** is a zero-configuration Spring Boot framework designed to bring this paradigm to the enterprise Java ecosystem. It elevates the standard Spring AI `ChatClient` from a simple request-response mechanism into a highly observable, stateful, and secure autonomous loop.

Prompting does not disappear — it is abstracted. Developers become **loop architects** who design goals, tool circuits, and safety guardrails. The loop handles tactical self-correction until the goal is met.

## Core Capabilities

### The AgentLoopManager

Moving beyond basic recursive interceptors, this framework decorates Spring AI's native tool execution with an advanced loop manager. It tracks per-turn state (`AgentTurn`) and enforces strict interaction budgets using **soft** and **hard** round bounds, eliminating the risk of infinite loops and runaway API billing. Tool-argument fingerprinting also blocks the model from retrying the exact same failed action.

### AG-UI Protocol Integration (Real-Time Frontend Streaming)

Long-running agent loops cannot rely on synchronous HTTP responses. This engine includes a reactive WebFlux layer that emits standardized AG-UI events (such as `TOOL_CALL_START` and `STATE_DELTA`) over Server-Sent Events, enabling frontends to render real-time progress. It also features built-in Human-in-the-Loop (HITL) suspension, pausing loop execution safely when human approval is required.

### Agent-to-Agent (A2A) Sub-Agent Spawning

Complex tasks degrade the performance of a single LLM. Integrated with `spring-ai-a2a` patterns, this framework allows your main orchestrator loop to autonomously discover capabilities via standard agent cards (`/.well-known/agent-card.json`) and spawn specialized, temporary sub-agents with strict round/token budgets.

### Zero-Trust Enterprise Governance

Built for production environments, the engine implements the **Vaquar Pattern for Data Mesh (PVDM)** Decision Attestation. Every tool execution is validated by an **MCP Bastion** that enforces Role-Based Access Control (RBAC), and generates a cryptographically signed audit trail of the agent's decision provenance. Output validation gates (density, dependency, design rules) run before the loop terminates.

### Observability & DLP

OpenTelemetry GenAI spans track input/output tokens, finish reasons, and agent spawn latency for Datadog, Grafana, and other APM tools. A `PiiMaskingSpanExporter` redacts SSNs, emails, and API keys before traces leave the network.

### AI-Assisted Development Ready (Cursor IDE)

Natively exposes your backend Java tools as an MCP server optimized for Cursor IDE. The starter includes utilities to auto-generate `mcp.json` configurations and inject project rules under `.cursor/rules/`, so Composer can work safely against your loop tools and governance gates.

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

## Modules

| Module | Description |
|--------|-------------|
| `spring-ai-loop-engine-core` | `AgentLoopManager`, `AgentTurn`, bounds, fingerprinting |
| `spring-ai-loop-engine-agui` | AG-UI SSE + HITL |
| `spring-ai-loop-engine-a2a` | Sub-agents + AgentCard |
| `spring-ai-loop-engine-mcp` | Bastion + Cursor `mcp.json` generator |
| `spring-ai-loop-engine-integrity` | Validation gates + PVDM attestation |
| `spring-ai-loop-engine-observability` | OTel GenAI spans + PII masking |
| `spring-ai-starter-loop-engine` | One dependency for everything |
| `spring-ai-loop-engine-bom` | BOM |

## Naming (Spring AI Community)

| Item | Value |
|------|-------|
| Repository / artifact | `spring-ai-loop-engine` |
| Starter | `spring-ai-starter-loop-engine` |
| GroupId (pre-incubation) | `io.github.vaquarkhan` |
| License | Apache 2.0 |

Community proposal: [spring-ai-community/community#28](https://github.com/spring-ai-community/community/issues/28)

## Cursor IDE

See [docs/cursor-setup.md](docs/cursor-setup.md).

```bash
# Unix
./scripts/install.sh --tool cursor

# Windows PowerShell
./scripts/install.ps1 -Tool cursor
```

- `.cursor/rules/loop-engine.mdc` — always-on project rules  
- `.cursor/mcp.json` — MCP snippet for Composer  

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

Reference patterns: [ai-agent-java-sdk](https://github.com/vaquarkhan/ai-agent-java-sdk)

## License

Apache License 2.0
