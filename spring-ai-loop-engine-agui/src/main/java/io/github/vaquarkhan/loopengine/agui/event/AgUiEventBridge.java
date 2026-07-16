package io.github.vaquarkhan.loopengine.agui.event;

import io.github.vaquarkhan.loopengine.core.loop.LoopListener;
import io.github.vaquarkhan.loopengine.core.loop.ModelRound;
import io.github.vaquarkhan.loopengine.core.model.AgentTurn;
import io.github.vaquarkhan.loopengine.core.model.LoopResult;
import io.github.vaquarkhan.loopengine.core.model.ToolCallFingerprint;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Translates loop lifecycle callbacks into AG-UI SSE event streams.
 */
public class AgUiEventBridge implements LoopListener {

    private final Map<String, Sinks.Many<AgUiEvent>> sinks = new ConcurrentHashMap<>();

    public Flux<AgUiEvent> stream(String turnId) {
        return sinkFor(turnId).asFlux();
    }

    public void complete(String turnId) {
        Sinks.Many<AgUiEvent> sink = sinks.remove(turnId);
        if (sink != null) {
            sink.tryEmitComplete();
        }
    }

    private Sinks.Many<AgUiEvent> sinkFor(String turnId) {
        return sinks.computeIfAbsent(turnId, id -> Sinks.many().multicast().onBackpressureBuffer());
    }

    private void emit(AgentTurn turn, String type, Map<String, Object> payload) {
        sinkFor(turn.turnId()).tryEmitNext(AgUiEvent.of(type, turn.sessionId(), turn.turnId(), payload));
    }

    @Override
    public void onTurnStarted(AgentTurn turn) {
        emit(turn, AgUiEvent.RUN_STARTED, Map.of(
                "sessionId", turn.sessionId(),
                "userMessage", turn.userMessage()));
    }

    @Override
    public void onRoundStarted(AgentTurn turn, int round) {
        emit(turn, AgUiEvent.STATE_DELTA, Map.of("round", round));
    }

    @Override
    public void onSoftWrapInjected(AgentTurn turn, int round) {
        emit(turn, AgUiEvent.STATE_DELTA, Map.of("softWrap", true, "round", round));
    }

    @Override
    public void onToolCallStarted(AgentTurn turn, ModelRound.ToolInvocation invocation) {
        emit(turn, AgUiEvent.TOOL_CALL_START, Map.of(
                "toolCallId", invocation.id(),
                "toolName", invocation.toolName(),
                "arguments", invocation.arguments()));
    }

    @Override
    public void onToolCallCompleted(AgentTurn turn, ToolCallFingerprint fingerprint) {
        emit(turn, AgUiEvent.TOOL_CALL_END, Map.of(
                "toolName", fingerprint.toolName(),
                "success", fingerprint.success(),
                "round", fingerprint.round()));
    }

    @Override
    public void onTurnCompleted(LoopResult result) {
        emit(result.turn(), AgUiEvent.TEXT_MESSAGE, Map.of("content", result.content()));
        emit(result.turn(), AgUiEvent.RUN_FINISHED, Map.of(
                "terminationReason", result.terminationReason().name(),
                "rounds", result.roundsExecuted()));
        complete(result.turn().turnId());
    }

    @Override
    public void onTurnFailed(AgentTurn turn, Throwable error) {
        emit(turn, AgUiEvent.ERROR, Map.of(
                "message", error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage()));
        complete(turn.turnId());
    }

    public void emitApprovalRequired(AgentTurn turn, String approvalId, String toolName, String arguments) {
        emit(turn, AgUiEvent.APPROVAL_REQUIRED, Map.of(
                "approvalId", approvalId,
                "toolName", toolName,
                "arguments", arguments));
    }
}
