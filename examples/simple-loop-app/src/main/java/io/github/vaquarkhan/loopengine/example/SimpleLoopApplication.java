package io.github.vaquarkhan.loopengine.example;

import io.github.vaquarkhan.loopengine.a2a.SubAgentSpawner;
import io.github.vaquarkhan.loopengine.core.loop.AgentLoopManager;
import io.github.vaquarkhan.loopengine.core.loop.LoopModelClient;
import io.github.vaquarkhan.loopengine.core.loop.LoopRequest;
import io.github.vaquarkhan.loopengine.core.loop.ModelRound;
import io.github.vaquarkhan.loopengine.core.model.AgentTurn;
import io.github.vaquarkhan.loopengine.core.model.LoopResult;
import io.github.vaquarkhan.loopengine.core.tool.ToolExecutor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Showcase demo for Spring AI Loop Engine — no LLM API key required.
 *
 * <p>Exercises multi-tool loops, soft wrap-up, duplicate-failure blocking,
 * A2A sub-agent spawn, and the standard AG-UI / AgentCard endpoints from the starter.
 */
@SpringBootApplication
public class SimpleLoopApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimpleLoopApplication.class, args);
    }

    /**
     * Scenario-aware demo model: supports happy-path tools, soft wrap, and duplicate retries.
     */
    @Bean
    @Primary
    LoopModelClient demoLoopModelClient() {
        return new DemoLoopModelClient();
    }

    @Bean
    ToolExecutor demoToolExecutor() {
        return (turn, invocation) -> {
            String name = invocation.toolName();
            if ("broken".equals(name)) {
                return ToolExecutor.ToolResult.failure("{\"error\":\"simulated_failure\",\"tool\":\"broken\"}");
            }
            if ("lookup".equals(name)) {
                return ToolExecutor.ToolResult.ok("{\"invoice\":\"INV-42\",\"status\":\"OPEN\",\"amount\":1200}");
            }
            return ToolExecutor.ToolResult.ok("{\"echo\":" + invocation.arguments() + "}");
        };
    }

    /**
     * Demo client that reads the latest user message to pick a scenario.
     */
    static final class DemoLoopModelClient implements LoopModelClient {

        @Override
        public ModelRound generate(AgentTurn turn, List<LoopMessage> messages, boolean softWrapUp) {
            String user = messages.stream()
                    .filter(m -> "user".equals(m.role()))
                    .reduce((a, b) -> b)
                    .map(LoopMessage::content)
                    .orElse("")
                    .toLowerCase();

            long toolMessages = messages.stream().filter(m -> "tool".equals(m.role())).count();

            if (softWrapUp) {
                return ModelRound.finalAnswer(
                        "Soft wrap-up: summarizing progress after budget pressure. rounds=" + turn.round());
            }

            if (user.contains("duplicate") || user.contains("retry-same")) {
                // Always request the same failing tool — engine should block the second attempt.
                return ModelRound.tools(List.of(
                        new ModelRound.ToolInvocation("t-broken", "broken", "{\"same\":true}")));
            }

            if (user.contains("multi") || user.contains("invoice")) {
                if (toolMessages == 0) {
                    return ModelRound.tools(List.of(
                            new ModelRound.ToolInvocation("t1", "lookup", "{\"q\":\"INV-42\"}")));
                }
                if (toolMessages == 1) {
                    return ModelRound.tools(List.of(
                            new ModelRound.ToolInvocation("t2", "echo", "{\"note\":\"looked up INV-42\"}")));
                }
                return ModelRound.finalAnswer(
                        "Invoice INV-42 is OPEN for 1200. Loop finished after multi-tool work. rounds="
                                + turn.round());
            }

            // Default happy path: one echo tool then finish
            if (toolMessages == 0) {
                return ModelRound.tools(List.of(
                        new ModelRound.ToolInvocation("demo", "echo", "{\"text\":\"loop-engine\"}")));
            }
            return ModelRound.finalAnswer(
                    "Loop complete. Soft wrap=" + softWrapUp + ", rounds so far=" + turn.round());
        }
    }

    @RestController
    static class DemoController {

        private final AgentLoopManager loopManager;
        private final SubAgentSpawner subAgentSpawner;
        private final AtomicInteger runs = new AtomicInteger();

        DemoController(AgentLoopManager loopManager, SubAgentSpawner subAgentSpawner) {
            this.loopManager = loopManager;
            this.subAgentSpawner = subAgentSpawner;
        }

        @GetMapping("/")
        Map<String, Object> index() {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("name", "spring-ai-loop-engine demo");
            body.put("hint", "POST /api/loop/run with message scenarios below");
            body.put("scenarios", List.of(
                    "default — one echo tool then complete",
                    "invoice / multi — lookup then echo then complete",
                    "duplicate / retry-same — same failing tool (fingerprint block)",
                    "wrap — use softMaxRounds=1 via query to force soft wrap"));
            body.put("endpoints", List.of(
                    "POST /api/loop/run",
                    "POST /api/loop/run/worker",
                    "POST /api/loop/ag-ui",
                    "GET /.well-known/agent-card.json",
                    "GET /api/demo/status"));
            body.put("runs", runs.get());
            return body;
        }

        @GetMapping("/api/demo/status")
        Map<String, Object> status() {
            return Map.of(
                    "ok", true,
                    "runs", runs.get(),
                    "workersSpawned", subAgentSpawner.spawnCount(),
                    "protocols", List.of("AG-UI", "A2A", "MCP"));
        }

        @PostMapping("/api/loop/run")
        Map<String, Object> run(
                @RequestBody Map<String, String> body,
                @RequestParam(name = "softMaxRounds", required = false) Integer softMaxRounds,
                @RequestParam(name = "hardMaxRounds", required = false) Integer hardMaxRounds) {
            runs.incrementAndGet();
            var builder = LoopRequest.builder()
                    .sessionId(body.getOrDefault("sessionId", "demo"))
                    .userMessage(body.getOrDefault("message", "Demonstrate the loop engine"))
                    .systemPrompt("You are a demo agent for Spring AI Loop Engine.");
            if (softMaxRounds != null) {
                builder.softMaxRounds(softMaxRounds);
            }
            if (hardMaxRounds != null) {
                builder.hardMaxRounds(hardMaxRounds);
            }

            LoopResult result = loopManager.run(builder.build());
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("content", result.content());
            response.put("terminationReason", result.terminationReason().name());
            response.put("rounds", result.roundsExecuted());
            response.put("tools", result.toolHistory().size());
            response.put("toolNames", result.toolHistory().stream()
                    .map(t -> t.toolName() + (t.success() ? ":ok" : ":fail"))
                    .toList());
            response.put("softWrapInjected", result.turn().isSoftWrapInjected());
            response.put("turnId", result.turn().turnId());
            return response;
        }

        @PostMapping("/api/loop/run/worker")
        Map<String, Object> spawnWorker(@RequestBody Map<String, String> body) {
            runs.incrementAndGet();
            LoopResult result = subAgentSpawner.spawn(
                    body.getOrDefault("message", "invoice multi tool demo"),
                    "You are a focused worker sub-agent.",
                    Integer.parseInt(body.getOrDefault("softMaxRounds", "4")),
                    Integer.parseInt(body.getOrDefault("hardMaxRounds", "6")));
            return Map.of(
                    "workerSession", result.turn().sessionId(),
                    "content", result.content(),
                    "terminationReason", result.terminationReason().name(),
                    "rounds", result.roundsExecuted(),
                    "tools", result.toolHistory().size(),
                    "spawnCount", subAgentSpawner.spawnCount());
        }
    }
}
