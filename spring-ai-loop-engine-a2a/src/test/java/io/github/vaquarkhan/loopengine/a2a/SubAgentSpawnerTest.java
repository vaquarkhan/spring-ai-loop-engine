package io.github.vaquarkhan.loopengine.a2a;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.vaquarkhan.loopengine.core.config.LoopEngineProperties;
import io.github.vaquarkhan.loopengine.core.loop.AgentLoopManager;
import io.github.vaquarkhan.loopengine.core.loop.LoopModelClient;
import io.github.vaquarkhan.loopengine.core.loop.ModelRound;
import io.github.vaquarkhan.loopengine.core.model.LoopResult;
import io.github.vaquarkhan.loopengine.core.model.TerminationReason;
import io.github.vaquarkhan.loopengine.core.tool.ToolExecutor;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SubAgentSpawnerTest {

    @Test
    void spawnUsesRequestBudgetsAndIncrementsCount() {
        AtomicInteger roundsSeen = new AtomicInteger();
        LoopModelClient client = (turn, messages, soft) -> {
            roundsSeen.set(turn.round());
            return ModelRound.finalAnswer("worker-done");
        };
        LoopEngineProperties props = new LoopEngineProperties();
        props.setSoftMaxRounds(15);
        props.setHardMaxRounds(25);
        AgentLoopManager manager = new AgentLoopManager(
                client, (t, i) -> ToolExecutor.ToolResult.ok("ok"), props, List.of());

        SubAgentSpawner spawner = new SubAgentSpawner(manager, props);
        LoopResult result = spawner.spawn("do work", "worker prompt", 2, 3);

        assertThat(result.content()).isEqualTo("worker-done");
        assertThat(result.terminationReason()).isEqualTo(TerminationReason.MODEL_COMPLETION);
        assertThat(result.turn().sessionId()).startsWith("worker-");
        assertThat(spawner.spawnCount()).isEqualTo(1);
        // Global props unchanged
        assertThat(props.getSoftMaxRounds()).isEqualTo(15);
        assertThat(props.getHardMaxRounds()).isEqualTo(25);
    }
}
