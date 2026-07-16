# simple-loop-app

Minimal Spring Boot example for Spring AI Loop Engine.

Uses a **demo** `LoopModelClient` and `ToolExecutor` so you can exercise the loop without an LLM API key. Pulls in `spring-ai-starter-loop-engine` and runs as a reactive WebFlux app.

## Run

```bash
# from repo root
mvn -pl examples/simple-loop-app -am install -DskipTests
mvn -pl examples/simple-loop-app spring-boot:run
```

Default port: `8080`.

## Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/api/loop/run` | Synchronous loop run (demo controller) |
| `POST` | `/api/loop/ag-ui` | AG-UI SSE stream |
| `POST` | `/api/loop/approvals/{id}` | HITL approval callback |
| `GET` | `/.well-known/agent-card.json` | A2A-style AgentCard |

### Example

```bash
curl -X POST http://localhost:8080/api/loop/run \
  -H "Content-Type: application/json" \
  -d "{\"sessionId\":\"demo\",\"message\":\"validate loop\"}"
```

Typical response:

```json
{
  "content": "Loop complete. Soft wrap=false, rounds so far=2",
  "terminationReason": "MODEL_COMPLETION",
  "rounds": 2,
  "tools": 1
}
```

## Configuration

See `src/main/resources/application.properties`:

- `spring.ai.loop.soft-max-rounds=5`
- `spring.ai.loop.hard-max-rounds=10`
- `spring.main.web-application-type=reactive`

## Production note

Replace the demo `LoopModelClient` with `ChatClientLoopModelClient` backed by a real Spring AI `ChatClient.Builder`, and register real `ToolCallback` / MCP tools instead of the echo demo tool.
