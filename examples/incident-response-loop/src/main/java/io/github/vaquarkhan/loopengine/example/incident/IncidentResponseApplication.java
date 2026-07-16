package io.github.vaquarkhan.loopengine.example.incident;

import io.github.vaquarkhan.loopengine.core.loop.AgentLoopManager;
import io.github.vaquarkhan.loopengine.core.loop.LoopModelClient;
import io.github.vaquarkhan.loopengine.core.loop.LoopRequest;
import io.github.vaquarkhan.loopengine.core.loop.ModelRound;
import io.github.vaquarkhan.loopengine.core.model.AgentTurn;
import io.github.vaquarkhan.loopengine.core.model.LoopResult;
import io.github.vaquarkhan.loopengine.core.tool.ToolExecutor;
import io.github.vaquarkhan.loopengine.mcp.bastion.ToolPermissionEvaluator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Real-world SRE use case: investigate an incident, then attempt remediation.
 * MCP Bastion RBAC allows {@code restart_service} only for actor {@code sre-oncall}.
 */
@SpringBootApplication
public class IncidentResponseApplication {

    /** Demo actor propagated from the HTTP request (gateway / IdP principal stand-in). */
    static final class ActorContext {
        private static final ThreadLocal<String> ACTOR = ThreadLocal.withInitial(() -> "anonymous");

        static void set(String actor) {
            ACTOR.set(actor == null || actor.isBlank() ? "anonymous" : actor);
        }

        static String get() {
            return ACTOR.get();
        }

        static void clear() {
            ACTOR.remove();
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(IncidentResponseApplication.class, args);
    }

    @Bean
    @Primary
    LoopModelClient incidentModelClient() {
        return new IncidentLoopModelClient();
    }

    /**
     * Read tools for everyone; destructive restart only for sre-oncall (via ActorContext).
     */
    @Bean
    ToolPermissionEvaluator incidentPermissions() {
        Set<String> readTools = Set.of("get_alerts", "query_logs", "check_slo");
        return (principal, toolName) -> {
            String actor = ActorContext.get();
            if (readTools.contains(toolName)) {
                return true;
            }
            if ("restart_service".equals(toolName)) {
                return "sre-oncall".equals(actor);
            }
            return false;
        };
    }

    @Bean
    ToolExecutor incidentTools() {
        return (turn, invocation) -> switch (invocation.toolName()) {
            case "get_alerts" -> ToolExecutor.ToolResult.ok(
                    "{\"alert\":\"checkout-p99-latency\",\"severity\":\"SEV-2\",\"service\":\"checkout\",\"since\":\"12m\"}");
            case "query_logs" -> ToolExecutor.ToolResult.ok(
                    "{\"hits\":3,\"pattern\":\"ConnectionPoolTimeout\",\"host\":\"checkout-7f9\",\"hint\":\"db pool exhaustion\"}");
            case "check_slo" -> ToolExecutor.ToolResult.ok(
                    "{\"slo\":\"checkout_availability\",\"burnRate\":2.4,\"errorBudgetRemaining\":\"18%\"}");
            case "restart_service" -> ToolExecutor.ToolResult.ok(
                    "{\"service\":\"checkout\",\"action\":\"rolling_restart\",\"status\":\"STARTED\",\"ticket\":\"CHG-2201\"}");
            default -> ToolExecutor.ToolResult.failure(
                    "{\"error\":\"unknown_tool\",\"tool\":\"" + invocation.toolName() + "\"}");
        };
    }

    static final class IncidentLoopModelClient implements LoopModelClient {
        @Override
        public ModelRound generate(AgentTurn turn, List<LoopMessage> messages, boolean softWrapUp) {
            if (softWrapUp) {
                return ModelRound.finalAnswer(
                        "Soft wrap: stop automated actions. Page on-call with gathered evidence. rounds="
                                + turn.round());
            }

            String user = lastUser(messages).toLowerCase();
            long tools = messages.stream().filter(m -> "tool".equals(m.role())).count();
            boolean remediate = user.contains("restart") || user.contains("remediat") || user.contains("fix");
            boolean readonly = user.contains("readonly") || user.contains("diagnose only");

            if (tools == 0) {
                return ModelRound.tools(List.of(new ModelRound.ToolInvocation(
                        "t1", "get_alerts", "{\"service\":\"checkout\"}")));
            }
            if (tools == 1) {
                return ModelRound.tools(List.of(new ModelRound.ToolInvocation(
                        "t2", "query_logs", "{\"service\":\"checkout\",\"window\":\"15m\"}")));
            }
            if (tools == 2) {
                return ModelRound.tools(List.of(new ModelRound.ToolInvocation(
                        "t3", "check_slo", "{\"service\":\"checkout\"}")));
            }
            if (tools == 3 && remediate && !readonly) {
                return ModelRound.tools(List.of(new ModelRound.ToolInvocation(
                        "t4", "restart_service", "{\"service\":\"checkout\",\"mode\":\"rolling\"}")));
            }

            boolean denied = messages.stream()
                    .filter(m -> "tool".equals(m.role()))
                    .map(LoopMessage::content)
                    .anyMatch(c -> c != null && c.contains("rbac_denied"));

            if (denied) {
                return ModelRound.finalAnswer(
                        "SEV-2 checkout latency: DB pool timeouts, SLO burn 2.4x. "
                                + "Bastion denied restart_service for this actor. Page sre-oncall to approve remediation.");
            }
            if (remediate && !readonly) {
                return ModelRound.finalAnswer(
                        "SEV-2 mitigated: rolling restart of checkout started (CHG-2201). "
                                + "Root signal: ConnectionPoolTimeout. Monitor p99 for 15 minutes.");
            }
            return ModelRound.finalAnswer(
                    "Diagnosis only: SEV-2 checkout p99 latency, ConnectionPoolTimeout on checkout-7f9, "
                            + "SLO burn 2.4x / 18% budget left. Recommend pool resize; no restart attempted.");
        }

        private static String lastUser(List<LoopMessage> messages) {
            return messages.stream()
                    .filter(m -> "user".equals(m.role()))
                    .reduce((a, b) -> b)
                    .map(LoopMessage::content)
                    .orElse("");
        }
    }

    @RestController
    static class Api {
        private final AgentLoopManager loops;
        private final AtomicInteger runs = new AtomicInteger();

        Api(AgentLoopManager loops) {
            this.loops = loops;
        }

        @GetMapping("/")
        Map<String, Object> index() {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("useCase", "SRE incident response — diagnose, then Bastion-gated remediation");
            body.put("port", 8083);
            body.put("actors", List.of(
                    "anonymous / analyst — read tools only; restart denied",
                    "sre-oncall — may call restart_service"));
            body.put("scenarios", List.of(
                    "POST {\"message\":\"diagnose only checkout latency\",\"actor\":\"analyst\"}",
                    "POST {\"message\":\"remediate restart checkout\",\"actor\":\"analyst\"} — RBAC deny",
                    "POST {\"message\":\"remediate restart checkout\",\"actor\":\"sre-oncall\"} — restart ok"));
            body.put("endpoint", "POST /api/incident");
            body.put("runs", runs.get());
            return body;
        }

        @PostMapping("/api/incident")
        Mono<Map<String, Object>> incident(@RequestBody Map<String, String> body) {
            return Mono.fromCallable(() -> {
                runs.incrementAndGet();
                ActorContext.set(body.getOrDefault("actor", "anonymous"));
                try {
                    LoopResult result = loops.run(LoopRequest.builder()
                            .sessionId(body.getOrDefault("sessionId", "sre-incident"))
                            .userMessage(body.getOrDefault("message", "diagnose only checkout latency"))
                            .systemPrompt("You are an SRE incident agent. Use tools; respect Bastion denials.")
                            .build());
                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("actor", ActorContext.get());
                    response.put("content", result.content());
                    response.put("terminationReason", result.terminationReason().name());
                    response.put("rounds", result.roundsExecuted());
                    response.put("tools", result.toolHistory().stream()
                            .map(t -> t.toolName() + (t.success() ? ":ok" : ":fail"))
                            .toList());
                    return response;
                }
                finally {
                    ActorContext.clear();
                }
            }).subscribeOn(Schedulers.boundedElastic());
        }
    }
}
