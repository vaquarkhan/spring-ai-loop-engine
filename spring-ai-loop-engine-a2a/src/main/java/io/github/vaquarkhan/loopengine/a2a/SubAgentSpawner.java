package io.github.vaquarkhan.loopengine.a2a;

import io.github.vaquarkhan.loopengine.core.config.LoopEngineProperties;
import io.github.vaquarkhan.loopengine.core.loop.AgentLoopManager;
import io.github.vaquarkhan.loopengine.core.loop.LoopRequest;
import io.github.vaquarkhan.loopengine.core.model.LoopResult;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spawns temporary worker sub-agents with strict round budgets from the orchestrator loop.
 */
public class SubAgentSpawner {

    private static final Logger log = LoggerFactory.getLogger(SubAgentSpawner.class);

    private final AgentLoopManager loopManager;
    private final LoopEngineProperties properties;
    private final AtomicInteger spawnCount = new AtomicInteger();

    public SubAgentSpawner(AgentLoopManager loopManager, LoopEngineProperties properties) {
        this.loopManager = Objects.requireNonNull(loopManager);
        this.properties = Objects.requireNonNull(properties);
    }

    public LoopResult spawn(String goal, String systemPrompt, int softMaxRounds, int hardMaxRounds) {
        String workerId = "worker-" + UUID.randomUUID();
        spawnCount.incrementAndGet();
        log.info("Spawning sub-agent {} (soft={}, hard={}, tokenBudget~={})",
                workerId, softMaxRounds, hardMaxRounds, properties.getA2a().getDefaultSubAgentTokenBudget());

        return loopManager.run(LoopRequest.builder()
                .sessionId(workerId)
                .userMessage(goal)
                .systemPrompt(systemPrompt == null || systemPrompt.isBlank()
                        ? "You are a focused worker sub-agent. Complete the goal and stop."
                        : systemPrompt)
                .softMaxRounds(Math.max(1, softMaxRounds))
                .hardMaxRounds(Math.max(softMaxRounds, hardMaxRounds))
                .build());
    }

    public int spawnCount() {
        return spawnCount.get();
    }
}
