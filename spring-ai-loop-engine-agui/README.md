# spring-ai-loop-engine-agui

Agent-User Interaction (AG-UI) streaming and Human-in-the-Loop (HITL) for long-running loops.

Long-running loops cannot rely on synchronous HTTP. This module streams standardized AG-UI events over WebFlux SSE and can pause execution until a human approves a sensitive tool call.

## Artifact

```xml
<dependency>
  <groupId>io.github.vaquarkhan</groupId>
  <artifactId>spring-ai-loop-engine-agui</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Requires a **reactive** web application (`spring-boot-starter-webflux`).

## Package layout

| Package | Contents |
|---------|----------|
| `...agui.event` | `AgUiEvent`, `AgUiEventBridge` (`LoopListener`) |
| `...agui.hitl` | `HitlApprovalStore` |
| `...agui.web` | `AgUiSseController` |
| `...agui` | `AgUiAutoConfiguration` |

## Key classes

- **`AgUiEventBridge`** — translates loop lifecycle callbacks into AG-UI SSE events
- **`HitlApprovalStore`** — suspends a turn, waits for approval, then resumes
- **`AgUiSseController`** — WebFlux endpoints for run streaming and approval callbacks

## Events emitted

| Event type | When |
|------------|------|
| `RunStartedEvent` | Loop turn starts |
| `STATE_DELTA` | Round / soft-wrap updates |
| `TOOL_CALL_START` / `TOOL_CALL_END` | Tool lifecycle |
| `TEXT_MESSAGE` | Final content |
| `RunFinishedEvent` | Turn completed |
| `APPROVAL_REQUIRED` | HITL pause (when requested) |
| `ERROR` | Turn failed |

## Endpoints

| Method | Path (default) | Purpose |
|--------|----------------|---------|
| `POST` | `/api/loop/ag-ui` | Start loop and stream AG-UI SSE |
| `GET` | `/api/loop/ag-ui/{turnId}` | Subscribe to an existing turn stream |
| `POST` | `/api/loop/approvals/{approvalId}` | Approve / deny a pending tool call |

## Configuration

| Property | Default |
|----------|---------|
| `spring.ai.loop.agui.enabled` | `true` |
| `spring.ai.loop.agui.sse-path` | `/api/loop/ag-ui` |
| `spring.ai.loop.agui.approval-path` | `/api/loop/approvals` |

## Example

```bash
curl -N -X POST http://localhost:8080/api/loop/ag-ui \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"ui-1","message":"Show progress while working"}'
```
