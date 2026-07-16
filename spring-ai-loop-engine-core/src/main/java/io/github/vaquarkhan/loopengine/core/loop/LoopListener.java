package io.github.vaquarkhan.loopengine.core.loop;

import io.github.vaquarkhan.loopengine.core.model.AgentTurn;
import io.github.vaquarkhan.loopengine.core.model.LoopResult;
import io.github.vaquarkhan.loopengine.core.model.ToolCallFingerprint;

/**
 * Observer for loop lifecycle events (AG-UI streaming, OTel, attestation hooks).
 */
public interface LoopListener {

    default void onTurnStarted(AgentTurn turn) {
    }

    default void onRoundStarted(AgentTurn turn, int round) {
    }

    default void onSoftWrapInjected(AgentTurn turn, int round) {
    }

    default void onToolCallStarted(AgentTurn turn, ModelRound.ToolInvocation invocation) {
    }

    default void onToolCallCompleted(AgentTurn turn, ToolCallFingerprint fingerprint) {
    }

    default void onDuplicateToolBlocked(AgentTurn turn, ModelRound.ToolInvocation invocation, ToolCallFingerprint prior) {
    }

    default void onTurnCompleted(LoopResult result) {
    }

    default void onTurnFailed(AgentTurn turn, Throwable error) {
    }
}
