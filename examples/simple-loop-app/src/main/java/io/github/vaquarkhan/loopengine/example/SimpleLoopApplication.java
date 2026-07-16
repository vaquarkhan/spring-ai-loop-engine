package io.github.vaquarkhan.loopengine.example;

import io.github.vaquarkhan.loopengine.core.loop.AgentLoopManager;
import io.github.vaquarkhan.loopengine.core.loop.LoopModelClient;
import io.github.vaquarkhan.loopengine.core.loop.LoopRequest;
import io.github.vaquarkhan.loopengine.core.loop.ModelRound;
import io.github.vaquarkhan.loopengine.core.model.AgentTurn;
import io.github.vaquarkhan.loopengine.core.model.LoopResult;
import io.github.vaquarkhan.loopengine.core.tool.ToolExecutor;
import java.util.List;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class SimpleLoopApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimpleLoopApplication.class, args);
    }

    /**
     * Demo model client — no API key required. Replace with ChatClientLoopModelClient in production.
     */
    @Bean
    @Primary
    LoopModelClient demoLoopModelClient() {
        return new LoopModelClient() {
            @Override
            public ModelRound generate(AgentTurn turn, List<LoopMessage> messages, boolean softWrapUp) {
                boolean alreadyUsedTool = messages.stream().anyMatch(m -> "tool".equals(m.role()));
                if (!alreadyUsedTool && !softWrapUp) {
                    return ModelRound.tools(List.of(
                            new ModelRound.ToolInvocation("demo", "echo", "{\"text\":\"loop-engine\"}")));
                }
                return ModelRound.finalAnswer("Loop complete. Soft wrap=" + softWrapUp
                        + ", rounds so far=" + turn.round());
            }
        };
    }

    @Bean
    ToolExecutor demoToolExecutor() {
        return (turn, invocation) -> ToolExecutor.ToolResult.ok(
                "{\"echo\":" + invocation.arguments() + "}");
    }

    @RestController
    static class DemoController {
        private final AgentLoopManager loopManager;

        DemoController(AgentLoopManager loopManager) {
            this.loopManager = loopManager;
        }

        @PostMapping("/api/loop/run")
        Map<String, Object> run(@RequestBody Map<String, String> body) {
            LoopResult result = loopManager.run(LoopRequest.builder()
                    .sessionId(body.getOrDefault("sessionId", "demo"))
                    .userMessage(body.getOrDefault("message", "Demonstrate the loop engine"))
                    .systemPrompt("You are a demo agent.")
                    .build());
            return Map.of(
                    "content", result.content(),
                    "terminationReason", result.terminationReason().name(),
                    "rounds", result.roundsExecuted(),
                    "tools", result.toolHistory().size());
        }
    }
}
