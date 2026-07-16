package io.github.vaquarkhan.loopengine.example.support;

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
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Real-world CX use case: triage a support ticket, look up the customer, search KB, draft a reply.
 * Billing disputes can spawn a budgeted A2A specialist worker.
 */
@SpringBootApplication
public class SupportTriageApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupportTriageApplication.class, args);
    }

    @Bean
    @Primary
    LoopModelClient supportModelClient() {
        return new SupportLoopModelClient();
    }

    @Bean
    ToolExecutor supportTools() {
        return (turn, invocation) -> switch (invocation.toolName()) {
            case "get_ticket" -> ToolExecutor.ToolResult.ok(
                    "{\"ticketId\":\"TCK-5521\",\"subject\":\"Charged twice for March\",\"priority\":\"HIGH\",\"channel\":\"email\"}");
            case "get_customer" -> ToolExecutor.ToolResult.ok(
                    "{\"customerId\":\"CUS-88\",\"plan\":\"Business\",\"arr\":24000,\"csat\":4.2,\"vip\":true}");
            case "search_kb" -> ToolExecutor.ToolResult.ok(
                    "{\"articles\":[{\"id\":\"KB-12\",\"title\":\"Duplicate charge refund policy\",\"score\":0.91}]}");
            case "draft_reply" -> ToolExecutor.ToolResult.ok(
                    "{\"draftId\":\"DR-441\",\"tone\":\"empathetic\",\"status\":\"READY_FOR_AGENT_REVIEW\"}");
            case "escalate_l2" -> ToolExecutor.ToolResult.ok(
                    "{\"escalationId\":\"ESC-19\",\"queue\":\"BILLING_L2\",\"slaMinutes\":120}");
            default -> ToolExecutor.ToolResult.failure(
                    "{\"error\":\"unknown_tool\",\"tool\":\"" + invocation.toolName() + "\"}");
        };
    }

    static final class SupportLoopModelClient implements LoopModelClient {
        @Override
        public ModelRound generate(AgentTurn turn, List<LoopMessage> messages, boolean softWrapUp) {
            if (softWrapUp) {
                return ModelRound.finalAnswer(
                        "Soft wrap: triage incomplete. Queue ticket for human agent with gathered context. rounds="
                                + turn.round());
            }

            String user = lastUser(messages).toLowerCase();
            long tools = messages.stream().filter(m -> "tool".equals(m.role())).count();
            boolean billing = user.contains("billing") || user.contains("charge") || user.contains("refund");
            boolean escalate = user.contains("escalate");

            if (tools == 0) {
                return ModelRound.tools(List.of(new ModelRound.ToolInvocation(
                        "t1", "get_ticket", "{\"ticketId\":\"TCK-5521\"}")));
            }
            if (tools == 1) {
                return ModelRound.tools(List.of(new ModelRound.ToolInvocation(
                        "t2", "get_customer", "{\"customerId\":\"CUS-88\"}")));
            }
            if (tools == 2) {
                return ModelRound.tools(List.of(new ModelRound.ToolInvocation(
                        "t3", "search_kb", "{\"q\":\"duplicate charge refund\"}")));
            }
            if (tools == 3) {
                if (escalate) {
                    return ModelRound.tools(List.of(new ModelRound.ToolInvocation(
                            "t4", "escalate_l2", "{\"ticketId\":\"TCK-5521\",\"reason\":\"VIP billing dispute\"}")));
                }
                return ModelRound.tools(List.of(new ModelRound.ToolInvocation(
                        "t4", "draft_reply", "{\"ticketId\":\"TCK-5521\",\"template\":\"duplicate_charge\"}")));
            }

            if (escalate) {
                return ModelRound.finalAnswer(
                        "TCK-5521 (VIP) escalated to BILLING_L2 as ESC-19. SLA 120 minutes. "
                                + "Customer CUS-88 on Business plan — do not auto-refund without L2.");
            }
            if (billing) {
                return ModelRound.finalAnswer(
                        "Triage complete for TCK-5521. Likely duplicate March charge. "
                                + "Draft DR-441 ready for agent review using KB-12. VIP customer — prioritize.");
            }
            return ModelRound.finalAnswer(
                    "Triage complete for TCK-5521. Context gathered; draft ready for human send.");
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
        private final SubAgentSpawner spawner;
        private final AtomicInteger runs = new AtomicInteger();

        Api(AgentLoopManager loops, SubAgentSpawner spawner) {
            this.loops = loops;
            this.spawner = spawner;
        }

        @GetMapping("/")
        Map<String, Object> index() {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("useCase", "Customer support — ticket triage, KB search, draft reply / escalate");
            body.put("port", 8082);
            body.put("scenarios", List.of(
                    "POST /api/triage {\"message\":\"billing charge refund triage\"}",
                    "POST /api/triage {\"message\":\"escalate VIP billing dispute\"}",
                    "POST /api/triage/billing-specialist — A2A worker with tight budget"));
            body.put("runs", runs.get());
            body.put("workersSpawned", spawner.spawnCount());
            return body;
        }

        @PostMapping("/api/triage")
        Mono<Map<String, Object>> triage(@RequestBody Map<String, String> body) {
            return Mono.fromCallable(() -> {
                runs.incrementAndGet();
                LoopResult result = loops.run(LoopRequest.builder()
                        .sessionId(body.getOrDefault("sessionId", "cx-triage"))
                        .userMessage(body.getOrDefault("message", "billing charge refund triage"))
                        .systemPrompt("You are a support triage agent. Prefer tools; never invent refunds.")
                        .build());
                return toResponse(result);
            }).subscribeOn(Schedulers.boundedElastic());
        }

        @PostMapping("/api/triage/billing-specialist")
        Mono<Map<String, Object>> billingSpecialist(@RequestBody Map<String, String> body) {
            return Mono.fromCallable(() -> {
                runs.incrementAndGet();
                LoopResult result = spawner.spawn(
                        body.getOrDefault("message", "billing charge refund triage"),
                        "You are a billing specialist sub-agent. Stay within budget; recommend, do not refund.",
                        3,
                        5);
                Map<String, Object> response = toResponse(result);
                response.put("workerSession", result.turn().sessionId());
                response.put("spawnCount", spawner.spawnCount());
                return response;
            }).subscribeOn(Schedulers.boundedElastic());
        }

        private static Map<String, Object> toResponse(LoopResult result) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("content", result.content());
            response.put("terminationReason", result.terminationReason().name());
            response.put("rounds", result.roundsExecuted());
            response.put("tools", result.toolHistory().stream()
                    .map(t -> t.toolName() + (t.success() ? ":ok" : ":fail"))
                    .toList());
            return response;
        }
    }
}
