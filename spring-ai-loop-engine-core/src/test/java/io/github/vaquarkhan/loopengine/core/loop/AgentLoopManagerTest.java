package io.github.vaquarkhan.loopengine.core.loop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.vaquarkhan.loopengine.core.config.LoopEngineProperties;
import io.github.vaquarkhan.loopengine.core.model.AgentTurn;
import io.github.vaquarkhan.loopengine.core.model.LoopResult;
import io.github.vaquarkhan.loopengine.core.model.TerminationReason;
import io.github.vaquarkhan.loopengine.core.tool.ToolExecutor;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AgentLoopManagerTest {

    @Test
    void completesWhenModelReturnsFinalAnswer() {
        LoopModelClient client = (turn, messages, soft) -> ModelRound.finalAnswer("done");
        ToolExecutor tools = (turn, inv) -> ToolExecutor.ToolResult.ok("ok");
        AgentLoopManager manager = new AgentLoopManager(client, tools, props(3, 5), List.of());

        LoopResult result = manager.run(LoopRequest.builder().userMessage("hello").build());

        assertThat(result.content()).isEqualTo("done");
        assertThat(result.terminationReason()).isEqualTo(TerminationReason.MODEL_COMPLETION);
        assertThat(result.roundsExecuted()).isEqualTo(1);
    }

    @Test
    void executesToolsThenCompletes() {
        AtomicInteger calls = new AtomicInteger();
        LoopModelClient client = (turn, messages, soft) -> {
            if (calls.getAndIncrement() == 0) {
                return ModelRound.tools(List.of(new ModelRound.ToolInvocation("t1", "echo", "{\"x\":1}")));
            }
            return ModelRound.finalAnswer("after-tool");
        };
        ToolExecutor tools = (turn, inv) -> ToolExecutor.ToolResult.ok("{\"ok\":true}");
        AgentLoopManager manager = new AgentLoopManager(client, tools, props(5, 10), List.of());

        LoopResult result = manager.run(LoopRequest.builder().userMessage("run").build());

        assertThat(result.content()).isEqualTo("after-tool");
        assertThat(result.toolHistory()).hasSize(1);
        assertThat(result.toolHistory().getFirst().success()).isTrue();
    }

    @Test
    void blocksDuplicateFailedToolFingerprint() {
        AtomicInteger modelCalls = new AtomicInteger();
        AtomicInteger toolCalls = new AtomicInteger();
        LoopModelClient client = (turn, messages, soft) -> {
            int n = modelCalls.getAndIncrement();
            if (n < 2) {
                return ModelRound.tools(List.of(
                        new ModelRound.ToolInvocation("t1", "broken", "{\"same\":true}")));
            }
            return ModelRound.finalAnswer("recovered");
        };
        ToolExecutor tools = (turn, inv) -> {
            toolCalls.incrementAndGet();
            return ToolExecutor.ToolResult.failure("{\"error\":\"boom\"}");
        };
        AgentLoopManager manager = new AgentLoopManager(client, tools, props(5, 10), List.of());

        LoopResult result = manager.run(LoopRequest.builder().userMessage("retry").build());

        assertThat(toolCalls.get()).isEqualTo(1);
        assertThat(result.toolHistory()).hasSize(2);
        assertThat(result.toolHistory().get(1).outputSummary()).contains("duplicate_failed_action");
        assertThat(result.content()).isEqualTo("recovered");
    }

    @Test
    void injectsSoftWrapAndStopsTools() {
        AtomicInteger modelCalls = new AtomicInteger();
        LoopModelClient client = (turn, messages, soft) -> {
            int n = modelCalls.incrementAndGet();
            boolean sawWrap = messages.stream().anyMatch(m ->
                    m.content() != null && m.content().contains("soft interaction budget"));
            if (n == 1) {
                return ModelRound.tools(List.of(new ModelRound.ToolInvocation("t1", "echo", "{}")));
            }
            if (soft || sawWrap) {
                return ModelRound.finalAnswer("wrapping-up");
            }
            return ModelRound.tools(List.of(new ModelRound.ToolInvocation("t2", "echo", "{}")));
        };
        ToolExecutor tools = (turn, inv) -> ToolExecutor.ToolResult.ok("ok");
        LoopEngineProperties properties = props(2, 5);
        AgentLoopManager manager = new AgentLoopManager(client, tools, properties, List.of());

        LoopResult result = manager.run(LoopRequest.builder().userMessage("long").build());

        assertThat(result.terminationReason()).isIn(TerminationReason.SOFT_WRAP_UP, TerminationReason.MODEL_COMPLETION);
        assertThat(result.content()).isEqualTo("wrapping-up");
        assertThat(result.turn().isSoftWrapInjected()).isTrue();
    }

    @Test
    void throwsOnHardMaxRounds() {
        LoopModelClient client = (turn, messages, soft) ->
                ModelRound.tools(List.of(new ModelRound.ToolInvocation("t1", "echo", "{}")));
        ToolExecutor tools = (turn, inv) -> ToolExecutor.ToolResult.ok("ok");
        AgentLoopManager manager = new AgentLoopManager(client, tools, props(1, 2), List.of());

        assertThatThrownBy(() -> manager.run(LoopRequest.builder().userMessage("infinite").build()))
                .isInstanceOf(HardMaxRoundsExceededException.class)
                .satisfies(ex -> {
                    HardMaxRoundsExceededException hard = (HardMaxRoundsExceededException) ex;
                    AgentTurn turn = hard.turn();
                    assertThat(turn.terminationReason()).isEqualTo(TerminationReason.HARD_MAX_ROUNDS);
                    assertThat(hard.hardMaxRounds()).isEqualTo(2);
                });
    }

    private static LoopEngineProperties props(int soft, int hard) {
        LoopEngineProperties properties = new LoopEngineProperties();
        properties.setSoftMaxRounds(soft);
        properties.setHardMaxRounds(hard);
        properties.setBlockDuplicateFailedActions(true);
        return properties;
    }
}
