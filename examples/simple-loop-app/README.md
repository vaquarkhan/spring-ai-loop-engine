# simple-loop-app

Showcase Spring Boot demo for Spring AI Loop Engine — **no LLM API key required**.

Uses a scenario-aware demo `LoopModelClient` plus echo/lookup/broken tools to exercise:

- multi-tool loops
- soft wrap-up budgets
- duplicate failed-action fingerprint blocking
- A2A sub-agent spawn
- AG-UI SSE + AgentCard + HITL approval endpoint

## Run

```bash
# from repo root (JDK 21+)
mvn -pl examples/simple-loop-app -am install -DskipTests
mvn -pl examples/simple-loop-app spring-boot:run
```

Open http://localhost:8080/ for the scenario index.

## Scenarios (`POST /api/loop/run`)

| Message contains | Behavior |
|------------------|----------|
| *(default)* | One `echo` tool then complete |
| `invoice` / `multi` | `lookup` → `echo` → final answer with INV-42 |
| `duplicate` / `retry-same` | Same failing `broken` tool; second call blocked; soft wrap |
| `wrap` + `?softMaxRounds=1` | Immediate soft wrap-up |

### Examples

```bash
# Happy path
curl -X POST http://localhost:8080/api/loop/run -H "Content-Type: application/json" -d "{\"message\":\"hello\"}"

# Multi-tool
curl -X POST http://localhost:8080/api/loop/run -H "Content-Type: application/json" -d "{\"message\":\"invoice multi tool demo\"}"

# Soft wrap
curl -X POST "http://localhost:8080/api/loop/run?softMaxRounds=1&hardMaxRounds=3" -H "Content-Type: application/json" -d "{\"message\":\"please wrap\"}"

# A2A worker
curl -X POST http://localhost:8080/api/loop/run/worker -H "Content-Type: application/json" -d "{\"message\":\"invoice multi\"}"

# AG-UI SSE
curl -N -X POST http://localhost:8080/api/loop/ag-ui -H "Content-Type: application/json" -d "{\"sessionId\":\"demo\",\"message\":\"invoice multi\"}"

# AgentCard
curl http://localhost:8080/.well-known/agent-card.json
```

## Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/` | Scenario index |
| `GET` | `/api/demo/status` | Run counters |
| `POST` | `/api/loop/run` | Synchronous loop (optional soft/hard query params) |
| `POST` | `/api/loop/run/worker` | Spawn budgeted sub-agent |
| `POST` | `/api/loop/ag-ui` | AG-UI SSE stream |
| `POST` | `/api/loop/approvals/{id}` | HITL approval callback |
| `GET` | `/.well-known/agent-card.json` | A2A-style AgentCard |

## Production note

Replace `DemoLoopModelClient` with `ChatClientLoopModelClient` and register real `ToolCallback` / MCP tools.
