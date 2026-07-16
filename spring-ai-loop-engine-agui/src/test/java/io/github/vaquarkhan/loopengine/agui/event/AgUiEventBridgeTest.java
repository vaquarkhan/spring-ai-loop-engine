package io.github.vaquarkhan.loopengine.agui.event;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.vaquarkhan.loopengine.core.loop.ModelRound;
import io.github.vaquarkhan.loopengine.core.model.AgentTurn;
import io.github.vaquarkhan.loopengine.core.model.LoopResult;
import io.github.vaquarkhan.loopengine.core.model.TerminationReason;
import io.github.vaquarkhan.loopengine.core.model.ToolCallFingerprint;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class AgUiEventBridgeTest {

    @Test
    void emitsLifecycleEventsInOrder() {
        AgUiEventBridge bridge = new AgUiEventBridge();
        AgentTurn turn = new AgentTurn("session-1", "hello");
        List<String> types = new ArrayList<>();

        StepVerifier.create(bridge.stream(turn.turnId()).doOnNext(e -> types.add(e.type())))
                .then(() -> {
                    bridge.onTurnStarted(turn);
                    bridge.onRoundStarted(turn, 1);
                    bridge.onToolCallStarted(turn, new ModelRound.ToolInvocation("1", "echo", "{}"));
                    bridge.onToolCallCompleted(turn, new ToolCallFingerprint(
                            "echo", "fp", "{}", true, "ok", Instant.now(), 1));
                    LoopResult result = new LoopResult(
                            "done", TerminationReason.MODEL_COMPLETION, 1, Duration.ofMillis(5), turn);
                    bridge.onTurnCompleted(result);
                })
                .expectNextCount(6)
                .verifyComplete();

        assertThat(types).containsExactly(
                AgUiEvent.RUN_STARTED,
                AgUiEvent.STATE_DELTA,
                AgUiEvent.TOOL_CALL_START,
                AgUiEvent.TOOL_CALL_END,
                AgUiEvent.TEXT_MESSAGE,
                AgUiEvent.RUN_FINISHED);    }

    @Test
    void emitApprovalRequired() {
        AgUiEventBridge bridge = new AgUiEventBridge();
        AgentTurn turn = new AgentTurn("s", "approve me");

        StepVerifier.create(bridge.stream(turn.turnId()))
                .then(() -> bridge.emitApprovalRequired(turn, "appr-1", "delete", "{\"id\":1}"))
                .assertNext(event -> {
                    assertThat(event.type()).isEqualTo(AgUiEvent.APPROVAL_REQUIRED);
                    assertThat(event.payload()).containsEntry("approvalId", "appr-1");
                    assertThat(event.payload()).containsEntry("toolName", "delete");
                })
                .thenCancel()
                .verify();
    }
}
