package io.github.vaquarkhan.loopengine.core.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Fingerprinted record of a tool invocation within an {@link AgentTurn}.
 * Used to prevent the LLM from retrying the exact same failed action.
 */
public final class ToolCallFingerprint {

    private final String toolName;
    private final String argumentsFingerprint;
    private final String rawArguments;
    private final boolean success;
    private final String outputSummary;
    private final Instant executedAt;
    private final int round;

    public ToolCallFingerprint(
            String toolName,
            String argumentsFingerprint,
            String rawArguments,
            boolean success,
            String outputSummary,
            Instant executedAt,
            int round) {
        this.toolName = Objects.requireNonNull(toolName, "toolName");
        this.argumentsFingerprint = Objects.requireNonNull(argumentsFingerprint, "argumentsFingerprint");
        this.rawArguments = rawArguments == null ? "" : rawArguments;
        this.success = success;
        this.outputSummary = outputSummary == null ? "" : outputSummary;
        this.executedAt = executedAt == null ? Instant.now() : executedAt;
        this.round = round;
    }

    public String toolName() {
        return toolName;
    }

    public String argumentsFingerprint() {
        return argumentsFingerprint;
    }

    public String rawArguments() {
        return rawArguments;
    }

    public boolean success() {
        return success;
    }

    public String outputSummary() {
        return outputSummary;
    }

    public Instant executedAt() {
        return executedAt;
    }

    public int round() {
        return round;
    }

    public String identityKey() {
        return toolName + "|" + argumentsFingerprint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ToolCallFingerprint that)) {
            return false;
        }
        return Objects.equals(identityKey(), that.identityKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(identityKey());
    }
}
