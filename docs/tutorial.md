# Spring AI Loop Engine — Developer Tutorial

Hands-on guide from first run to production-style patterns. Pair with the [Developer Guide](developer-guide.md) for full property/API tables.

**Time:** ~60–90 minutes if you complete every lab.  
**You need:** JDK 21+, Maven, a terminal, a browser. An OpenAI (or other Spring AI) API key is only needed from Lab 3 onward.

---

## Learning path

| Lab | Goal | LLM key? |
|-----|------|----------|
| [0](#lab-0--clone-build-verify) | Clone, build, tests green | No |
| [1](#lab-1--run-the-demo-live--simulation) | Live demo + simulation awareness | No |
| [2](#lab-2--exercise-every-demo-scenario) | Multi-tool, soft wrap, fingerprint, A2A, AG-UI | No |
| [3](#lab-3--add-the-starter-to-your-app) | Dependency + config | Yes (or mock) |
| [4](#lab-4--first-AgentLoopManager-call) | `LoopRequest` / `LoopResult` | Yes |
| [5](#lab-5--register-tools-safely) | `ToolCallback`, engine owns tools | Yes |
| [6](#lab-6--soft--hard-budgets-in-practice) | Prove wrap-up and hard stop | Yes |
| [7](#lab-7--stream-progress-with-ag-ui) | SSE frontend contract | Yes |
| [8](#lab-8--bastion-rbac) | Allow-list principals/tools | Yes |
| [9](#lab-9--integrity-gates--durable-pvdm) | Gates + HMAC secret | Yes |
| [10](#lab-10--otel--pii-masking) | Spans + exporter wrap | Yes |
| [11](#lab-11--a2a-spawn--agentcard) | Worker budgets + discovery | Yes |
| [12](#lab-12--custom-looplistener) | Extension SPI | Yes |
| [13](#lab-13--hitl-pattern-manual) | Approve destructive tools today | Yes |
| [14](#lab-14--production-checklist-walkthrough) | Ship confidently | — |

---

## Lab 0 — Clone, build, verify

```bash
git clone https://github.com/vaquarkhan/spring-ai-loop-engine.git
cd spring-ai-loop-engine
mvn verify
```

Expect `BUILD SUCCESS` (unit tests across core, agui, a2a, mcp, integrity, observability).

If Java is wrong:

```bash
java -version   # must be 21+
# Windows example:
# set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.x.x-hotspot
```

---

## Lab 1 — Run the demo (live + simulation)

### Why this lab exists

Opening the GitHub repo **does not** start the demo. Managers and developers need a clear path:

1. **Live** — real Loop Engine on your machine  
2. **Simulation** — sample outcomes when the server is off (or via offline HTML)

### Start live

```bash
mvn -pl examples/simple-loop-app -am install -DskipTests
mvn -pl examples/simple-loop-app spring-boot:run
```

Open [http://localhost:8080/](http://localhost:8080/).

You should see:

- A yellow **“not always live”** instruction box with the Maven commands  
- Mode badge (**Live** when `/api/demo/status` succeeds)  
- Checkbox **Always use simulation**

Click **Try invoice example**. Result panel should show multi-tool work (`lookup`, `echo`) and a final invoice summary.

### Simulation without a server

1. Stop the app (Ctrl+C).  
2. Refresh http://localhost:8080/ — connection fails → page falls back to **simulation**.  
3. Or open [`docs/demo-preview.html`](demo-preview.html) directly in a browser (works from a GitHub download).

**Checkpoint:** You can explain to a non-developer: “GitHub shows code; live needs build/start; clicks still work in simulation.”

---

## Lab 2 — Exercise every demo scenario

With the app running again:

### 2a. Default happy path

```bash
curl -s -X POST http://localhost:8080/api/loop/run \
  -H "Content-Type: application/json" \
  -d "{\"message\":\"hello\"}"
```

Expect one `echo` tool then `MODEL_COMPLETION`.

### 2b. Multi-tool invoice

```bash
curl -s -X POST http://localhost:8080/api/loop/run \
  -H "Content-Type: application/json" \
  -d "{\"message\":\"invoice multi tool demo\"}"
```

Expect `toolNames` like `lookup:ok`, `echo:ok`, content mentioning `INV-42`.

### 2c. Soft wrap budget

```bash
curl -s -X POST "http://localhost:8080/api/loop/run?softMaxRounds=1&hardMaxRounds=3" \
  -H "Content-Type: application/json" \
  -d "{\"message\":\"please wrap\"}"
```

Expect soft wrap language / `SOFT_WRAP_UP` (or soft-wrap injected flag).

### 2d. Duplicate failed action (fingerprint)

```bash
curl -s -X POST http://localhost:8080/api/loop/run \
  -H "Content-Type: application/json" \
  -d "{\"message\":\"duplicate retry-same\"}"
```

The demo model keeps requesting the same failing `broken` tool. The engine blocks the duplicate fingerprint and wraps up instead of infinite retries.

### 2e. A2A worker

```bash
curl -s -X POST http://localhost:8080/api/loop/run/worker \
  -H "Content-Type: application/json" \
  -d "{\"message\":\"invoice multi\"}"
```

Expect `workerSession` like `worker-...` and `spawnCount` ≥ 1.

### 2f. AG-UI SSE

```bash
curl -N -s -X POST http://localhost:8080/api/loop/ag-ui \
  -H "Content-Type: application/json" \
  -d "{\"sessionId\":\"demo\",\"message\":\"invoice multi\"}"
```

Watch for `RunStartedEvent`, `TOOL_CALL_START` / `END`, `STATE_DELTA`, `TEXT_MESSAGE`, `RunFinishedEvent`.

### 2g. AgentCard

```bash
curl -s http://localhost:8080/.well-known/agent-card.json
```

### 2h. Status

```bash
curl -s http://localhost:8080/api/demo/status
```

**Checkpoint:** You have exercised soft/hard concepts, fingerprinting, A2A, AG-UI, and discovery — all without a real LLM.

---

## Lab 3 — Add the starter to your app

Create a new Spring Boot 3.5.x WebFlux app (or use an existing one).

### Maven

```xml
<dependency>
  <groupId>io.github.vaquarkhan</groupId>
  <artifactId>spring-ai-starter-loop-engine</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

Install this repo first (`mvn -DskipTests install`) if the artifact is not on Maven Central yet.

### `application.yml`

```yaml
spring:
  main:
    web-application-type: reactive
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
    loop:
      soft-max-rounds: 10
      hard-max-rounds: 20
      block-duplicate-failed-actions: true
      agui:
        enabled: true
      a2a:
        enabled: true
        agent-name: my-loop-agent
      mcp:
        bastion-enabled: true
      integrity:
        enabled: true
      observability:
        enabled: true
server:
  port: 8080
```

**Checkpoint:** App starts; no bean errors for `AgentLoopManager`.

---

## Lab 4 — First `AgentLoopManager` call

```java
@RestController
public class LoopApi {
    private final AgentLoopManager loops;

    public LoopApi(AgentLoopManager loops) {
        this.loops = loops;
    }

    @PostMapping("/api/goals")
    public Map<String, Object> run(@RequestBody Map<String, String> body) {
        LoopResult result = loops.run(LoopRequest.builder()
            .sessionId(body.getOrDefault("sessionId", "default"))
            .userMessage(body.getOrDefault("message", "Say hello and stop."))
            .systemPrompt("You are a careful agent. Prefer tools when useful.")
            .build());

        return Map.of(
            "content", result.content(),
            "reason", result.terminationReason().name(),
            "rounds", result.roundsExecuted(),
            "tools", result.toolHistory().size()
        );
    }
}
```

Call:

```bash
curl -s -X POST http://localhost:8080/api/goals \
  -H "Content-Type: application/json" \
  -d "{\"message\":\"Reply with a one-sentence greeting.\"}"
```

**What happened:** Auto-config provided `ChatClientLoopModelClient` (tools executed by the engine, not Spring AI’s internal loop) and `AgentLoopManager` with soft/hard defaults from YAML.

---

## Lab 5 — Register tools safely

```java
@Configuration
public class ToolConfig {

    @Bean
    ToolCallback lookupTool() {
        return FunctionToolCallback.builder("lookup",
                (Map<String, Object> args) -> Map.of(
                    "invoice", args.getOrDefault("q", "unknown"),
                    "status", "OPEN",
                    "amount", 1200))
            .description("Look up an invoice by id")
            .inputType(Map.class)
            .build();
    }
}
```

(Use the `FunctionToolCallback` / `ToolCallback` style matching your Spring AI 1.1.x version.)

Ensure tools are discovered via a `ToolCallbackProvider` bean or registered into `ToolCallbackToolExecutor` as your Spring AI setup requires.

**Critical rule:** keep `ChatClientLoopModelClient` with **`internalToolExecutionEnabled=false`** (the default). If Spring AI executes tools internally, soft/hard budgets and fingerprinting may not apply.

Test by asking the model to look up `INV-42`. Inspect `result.toolHistory()`.

---

## Lab 6 — Soft & hard budgets in practice

### Soft wrap

```java
LoopResult soft = loops.run(LoopRequest.builder()
    .userMessage("Keep calling tools forever to research invoices")
    .softMaxRounds(2)
    .hardMaxRounds(10)
    .build());
```

At soft max the engine injects:

> You have reached the soft interaction budget for this turn. Do not call any more tools…

Further tool requests are suppressed; you should see wrap-up behavior / `SOFT_WRAP_UP`.

### Hard stop

Set `hardMaxRounds` very low **and** force continued tool use (custom `LoopModelClient` in a test is easiest — see `AgentLoopManagerTest` in the repo). Expect `HardMaxRoundsExceededException`.

**Rule of thumb:** soft = “please finish”; hard = “billing fuse.”

---

## Lab 7 — Stream progress with AG-UI

With AG-UI enabled and WebFlux:

```bash
curl -N -X POST http://localhost:8080/api/loop/ag-ui \
  -H "Content-Type: application/json" \
  -d "{\"sessionId\":\"ui-1\",\"message\":\"Use lookup for INV-42 then summarize\"}"
```

### Frontend sketch

```javascript
const res = await fetch('/api/loop/ag-ui', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ sessionId: 'ui-1', message: '...' })
});
const reader = res.body.getReader();
const dec = new TextDecoder();
// Parse SSE frames; switch on event.type:
// RunStartedEvent, STATE_DELTA, TOOL_CALL_START, TOOL_CALL_END,
// TEXT_MESSAGE, RunFinishedEvent, ERROR
```

Do **not** block the reactive event loop with synchronous `loops.run` in your own controllers — wrap with `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())` (the built-in AG-UI controller already does this).

---

## Lab 8 — Bastion RBAC

Default evaluator is **permit-all**. Replace it:

```java
@Bean
ToolPermissionEvaluator toolPermissions() {
    return ToolPermissionEvaluator.allowList(Map.of(
        "alice", Set.of("lookup"),
        "admin", Set.of("*"),
        "anonymous", Set.of()  // deny all if no login
    ));
}
```

With Spring Security, the principal name is used; otherwise `"anonymous"`.

Denied calls return failure JSON `rbac_denied` to the model (and can fingerprint if retried identically).

**Checkpoint:** `alice` can `lookup`; `anonymous` cannot.

---

## Lab 9 — Integrity gates & durable PVDM

Gates run after completion (density, dependency, YAML design). Violations land on turn attributes — treat as signals; they do not abort yet.

Provide a **stable** attestation secret:

```java
@Bean
DecisionAttestation.Signer pvdmSigner(
        @Value("${app.pvdm.hmac-secret}") String secret) {
    return DecisionAttestation.Signer.fromSecret(secret);
}
```

```yaml
app:
  pvdm:
    hmac-secret: ${PVDM_HMAC_SECRET}
```

Without this, each JVM restart gets a new random secret and old attestations will not verify.

Inspect attestations from `IntegrityLoopListener` (inject the listener bean and call `attestations()` in a secure admin endpoint, or log them in a custom listener).

---

## Lab 10 — OTel & PII masking

`GenAiLoopTelemetryListener` is auto-registered when observability is enabled. Export traces with your usual Spring Boot / OTel setup (OTLP to Grafana, Datadog, etc.).

Wrap the exporter:

```java
SpanExporter otlp = /* your OTLP exporter */;
SpanExporter safe = new PiiMaskingSpanExporter(otlp);
// register `safe` in SdkTracerProvider
```

Never log raw SSNs, emails, or API keys in custom span attributes.

---

## Lab 11 — A2A spawn & AgentCard

```java
@RestController
class Workers {
    private final SubAgentSpawner spawner;

    Workers(SubAgentSpawner spawner) {
        this.spawner = spawner;
    }

    @PostMapping("/api/workers")
    Map<String, Object> spawn(@RequestBody Map<String, String> body) {
        LoopResult r = spawner.spawn(
            body.get("goal"),
            "Focused worker. Stay within budget.",
            4,
            6);
        return Map.of(
            "session", r.turn().sessionId(),
            "content", r.content(),
            "rounds", r.roundsExecuted(),
            "spawnCount", spawner.spawnCount());
    }
}
```

Discover:

```bash
curl -s http://localhost:8080/.well-known/agent-card.json
```

For true multi-process A2A, compose with community `spring-ai-a2a` — this module gives **nested budgeted loops** + card metadata.

---

## Lab 12 — Custom `LoopListener`

```java
@Component
public class AuditLoopListener implements LoopListener {
    private static final Logger log = LoggerFactory.getLogger(AuditLoopListener.class);

    @Override
    public void onToolCallCompleted(AgentTurn turn, ToolCallFingerprint fp) {
        log.info("tool={} success={} turn={}", fp.toolName(), fp.success(), turn.turnId());
    }

    @Override
    public void onDuplicateToolBlocked(AgentTurn turn, String toolName, String fingerprint) {
        log.warn("blocked duplicate tool={} fp={} turn={}", toolName, fingerprint, turn.turnId());
    }

    @Override
    public void onSoftWrapInjected(AgentTurn turn) {
        log.info("soft wrap turn={} round={}", turn.turnId(), turn.round());
    }
}
```

All `LoopListener` beans are collected by `AgentLoopManager`.

---

## Lab 13 — HITL pattern (manual)

Built-in pieces: `HitlApprovalStore`, `POST /api/loop/approvals/{id}`, `APPROVAL_REQUIRED` event helper.

**Today the loop does not auto-pause** on sensitive tools. Pattern until that lands:

```java
@Bean
ToolExecutor hitlAwareExecutor(ToolExecutor delegate, HitlApprovalStore store, AgUiEventBridge bridge) {
    return (turn, invocation) -> {
        if ("transfer_funds".equals(invocation.toolName())) {
            var pending = store.requestApproval(turn, invocation.toolName(), invocation.arguments());
            bridge.emitApprovalRequired(turn.turnId(), pending.approvalId(), invocation.toolName(), invocation.arguments());
            var decision = pending.future().join(); // prefer async in real apps
            if (!decision.approved()) {
                return ToolExecutor.ToolResult.failure("{\"error\":\"approval_denied\"}");
            }
            // optionally use decision.modifiedArguments()
        }
        return delegate.execute(turn, invocation);
    };
}
```

UI: listen for `APPROVAL_REQUIRED`, show Approve/Deny, `POST` to `/api/loop/approvals/{approvalId}`.

Prefer non-blocking designs in production (timeouts, cancel, never `join` on the event loop).

---

## Lab 14 — Production checklist walkthrough

Work through [Developer Guide §15](developer-guide.md#15-production-checklist) and [§16 Known limitations](developer-guide.md#16-known-limitations-current-release).

Minimum bar before calling an app “production patterns”:

1. Soft + hard rounds set per env  
2. Bastion allow-list (not permit-all)  
3. Durable PVDM secret  
4. PII masking on the exporter  
5. AG-UI for long runs  
6. Explicit HITL for destructive tools  
7. Your own MCP server if IDEs call tools (generator alone is not enough)

---

## Quick reference — demo vs production

| Concern | Demo (`simple-loop-app`) | Production |
|---------|--------------------------|------------|
| Model | `@Primary DemoLoopModelClient` | `ChatClientLoopModelClient` + real key |
| Tools | Inline `ToolExecutor` | `ToolCallback` + Bastion allow-list |
| Key | Placeholder / none | Secret manager |
| UI | Manager HTML + simulation | Your app + AG-UI events |
| Attestation | Random secret OK | Fixed HMAC secret |

---

## Where to go next

- [Developer Guide](developer-guide.md) — full property & API reference  
- Module READMEs under each `spring-ai-loop-engine-*` folder  
- [CONTRIBUTING.md](../CONTRIBUTING.md) — tests and PR norms  
- Community proposal [#28](https://github.com/spring-ai-community/community/issues/28)

Questions or gaps in these labs? Open an issue on the repo with the lab number in the title.
