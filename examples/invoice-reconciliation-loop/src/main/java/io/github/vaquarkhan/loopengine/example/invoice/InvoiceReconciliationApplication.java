package io.github.vaquarkhan.loopengine.example.invoice;

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
 * Real-world finance use case: reconcile bank payments against open invoices.
 *
 * <p>No LLM API key required — a scenario-driven {@link LoopModelClient} drives the loop.
 */
@SpringBootApplication
public class InvoiceReconciliationApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvoiceReconciliationApplication.class, args);
    }

    @Bean
    @Primary
    LoopModelClient invoiceModelClient() {
        return new InvoiceLoopModelClient();
    }

    @Bean
    ToolExecutor invoiceTools() {
        return (turn, invocation) -> switch (invocation.toolName()) {
            case "fetch_invoice" -> ToolExecutor.ToolResult.ok(
                    "{\"invoiceId\":\"INV-1042\",\"vendor\":\"Acme Supplies\",\"amount\":1500.00,\"status\":\"OPEN\"}");
            case "fetch_payment" -> {
                String args = invocation.arguments() == null ? "" : invocation.arguments().toLowerCase();
                if (args.contains("mismatch") || args.contains("short")) {
                    yield ToolExecutor.ToolResult.ok(
                            "{\"paymentId\":\"PAY-77\",\"amount\":1450.00,\"ref\":\"INV-1042\",\"bank\":\"Chase\"}");
                }
                yield ToolExecutor.ToolResult.ok(
                        "{\"paymentId\":\"PAY-77\",\"amount\":1500.00,\"ref\":\"INV-1042\",\"bank\":\"Chase\"}");
            }
            case "post_match" -> ToolExecutor.ToolResult.ok(
                    "{\"matched\":true,\"invoiceId\":\"INV-1042\",\"paymentId\":\"PAY-77\",\"ledger\":\"AP\"}");
            case "open_exception" -> {
                String args = invocation.arguments() == null ? "" : invocation.arguments();
                if (args.contains("\"forceFail\":true") || args.contains("\"forceFail\": true")) {
                    yield ToolExecutor.ToolResult.failure(
                            "{\"error\":\"erp_unavailable\",\"code\":\"ERP_TIMEOUT\"}");
                }
                yield ToolExecutor.ToolResult.ok(
                        "{\"exceptionId\":\"EX-901\",\"invoiceId\":\"INV-1042\",\"delta\":50.00,\"queue\":\"AP_EXCEPTIONS\"}");
            }
            default -> ToolExecutor.ToolResult.failure(
                    "{\"error\":\"unknown_tool\",\"tool\":\"" + invocation.toolName() + "\"}");
        };
    }

    /**
     * Scenario keys in the user message:
     * <ul>
     *   <li>{@code match} / default — invoice + payment align → post_match</li>
     *   <li>{@code mismatch} / {@code short} — amounts differ → open_exception</li>
     *   <li>{@code retry} / {@code duplicate} — failing open_exception blocked by fingerprint</li>
     * </ul>
     */
    static final class InvoiceLoopModelClient implements LoopModelClient {
        @Override
        public ModelRound generate(AgentTurn turn, List<LoopMessage> messages, boolean softWrapUp) {
            if (softWrapUp) {
                return ModelRound.finalAnswer(
                        "Soft wrap: reconciliation paused. Escalate remaining items to AP analyst. rounds="
                                + turn.round());
            }

            String user = lastUser(messages).toLowerCase();
            long tools = messages.stream().filter(m -> "tool".equals(m.role())).count();

            boolean mismatch = user.contains("mismatch") || user.contains("short");
            boolean retry = user.contains("retry") || user.contains("duplicate");

            if (retry) {
                // Always the same failing args — engine should fingerprint-block the 2nd attempt
                return ModelRound.tools(List.of(new ModelRound.ToolInvocation(
                        "t-ex", "open_exception",
                        "{\"invoiceId\":\"INV-1042\",\"delta\":50,\"forceFail\":true}")));
            }

            if (tools == 0) {
                return ModelRound.tools(List.of(new ModelRound.ToolInvocation(
                        "t1", "fetch_invoice", "{\"invoiceId\":\"INV-1042\"}")));
            }
            if (tools == 1) {
                String payArgs = mismatch
                        ? "{\"ref\":\"INV-1042\",\"hint\":\"short\"}"
                        : "{\"ref\":\"INV-1042\"}";
                return ModelRound.tools(List.of(new ModelRound.ToolInvocation(
                        "t2", "fetch_payment", payArgs)));
            }
            if (tools == 2) {
                if (mismatch) {
                    return ModelRound.tools(List.of(new ModelRound.ToolInvocation(
                            "t3", "open_exception",
                            "{\"invoiceId\":\"INV-1042\",\"delta\":50.00,\"reason\":\"underpayment\"}")));
                }
                return ModelRound.tools(List.of(new ModelRound.ToolInvocation(
                        "t3", "post_match", "{\"invoiceId\":\"INV-1042\",\"paymentId\":\"PAY-77\"}")));
            }

            if (mismatch) {
                return ModelRound.finalAnswer(
                        "Invoice INV-1042 underpaid by 50.00. Exception EX-901 opened on AP_EXCEPTIONS. "
                                + "Human review required before write-off.");
            }
            return ModelRound.finalAnswer(
                    "Invoice INV-1042 matched to PAY-77 for 1500.00 and posted to AP ledger. Closed.");
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
            body.put("useCase", "Accounts payable — bank payment vs open invoice reconciliation");
            body.put("port", 8081);
            body.put("scenarios", List.of(
                    "POST {\"message\":\"reconcile INV-1042 match\"} — happy path post_match",
                    "POST {\"message\":\"reconcile INV-1042 mismatch short pay\"} — open_exception",
                    "POST {\"message\":\"retry duplicate open_exception\"} — fingerprint block"));
            body.put("endpoint", "POST /api/reconcile");
            body.put("runs", runs.get());
            return body;
        }

        @PostMapping("/api/reconcile")
        Mono<Map<String, Object>> reconcile(@RequestBody Map<String, String> body) {
            return Mono.fromCallable(() -> {
                runs.incrementAndGet();
                LoopResult result = loops.run(LoopRequest.builder()
                        .sessionId(body.getOrDefault("sessionId", "ap-recon"))
                        .userMessage(body.getOrDefault("message", "reconcile INV-1042 match"))
                        .systemPrompt("You are an AP reconciliation agent. Use tools; never invent ledger posts.")
                        .build());
                return toResponse(result);
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
            response.put("softWrapInjected", result.turn().isSoftWrapInjected());
            return response;
        }
    }
}
