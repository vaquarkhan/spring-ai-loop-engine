package io.github.vaquarkhan.loopengine.a2a;

import io.github.vaquarkhan.loopengine.core.config.LoopEngineProperties;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes {@code /.well-known/agent-card.json} for A2A discovery.
 */
@RestController
@ConditionalOnProperty(prefix = "spring.ai.loop.a2a", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AgentCardController {

    private final LoopEngineProperties properties;

    public AgentCardController(LoopEngineProperties properties) {
        this.properties = properties;
    }

    @GetMapping("${spring.ai.loop.a2a.agent-card-path:/.well-known/agent-card.json}")
    public AgentCard agentCard() {
        LoopEngineProperties.A2a a2a = properties.getA2a();
        return new AgentCard(
                a2a.getAgentName(),
                a2a.getAgentDescription(),
                "0.1.0-SNAPSHOT",
                "/",
                List.of("loop-engine", "tool-calling", "hitl", "soft-hard-bounds"),
                List.of(new AgentCard.Skill(
                        "agent-loop",
                        "Stateful Agent Loop",
                        "Run a budgeted autonomous tool loop for a high-level goal",
                        List.of("spring-ai", "loop-engineering"))),
                Map.of(
                        "softMaxRounds", properties.getSoftMaxRounds(),
                        "hardMaxRounds", properties.getHardMaxRounds(),
                        "protocols", List.of("AG-UI", "A2A", "MCP")));
    }
}
