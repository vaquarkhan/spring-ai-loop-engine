package io.github.vaquarkhan.loopengine.core.model;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Immutable result of a completed (or hard-stopped) agent loop.
 */
public final class LoopResult {

    private final String content;
    private final TerminationReason terminationReason;
    private final int roundsExecuted;
    private final Duration elapsed;
    private final AgentTurn turn;
    private final List<ToolCallFingerprint> toolHistory;

    public LoopResult(
            String content,
            TerminationReason terminationReason,
            int roundsExecuted,
            Duration elapsed,
            AgentTurn turn) {
        this.content = content == null ? "" : content;
        this.terminationReason = Objects.requireNonNull(terminationReason, "terminationReason");
        this.roundsExecuted = roundsExecuted;
        this.elapsed = elapsed == null ? Duration.ZERO : elapsed;
        this.turn = Objects.requireNonNull(turn, "turn");
        this.toolHistory = turn.toolHistory();
    }

    public String content() {
        return content;
    }

    public TerminationReason terminationReason() {
        return terminationReason;
    }

    public int roundsExecuted() {
        return roundsExecuted;
    }

    public Duration elapsed() {
        return elapsed;
    }

    public AgentTurn turn() {
        return turn;
    }

    public List<ToolCallFingerprint> toolHistory() {
        return toolHistory;
    }

    public boolean completedNormally() {
        return terminationReason == TerminationReason.MODEL_COMPLETION
                || terminationReason == TerminationReason.SOFT_WRAP_UP;
    }
}
