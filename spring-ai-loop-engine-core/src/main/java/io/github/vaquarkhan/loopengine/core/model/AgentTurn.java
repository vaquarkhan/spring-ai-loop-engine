package io.github.vaquarkhan.loopengine.core.model;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-user-message turn state tracked by {@code AgentLoopManager}.
 * Holds round count, tool history, fingerprints, and arbitrary loop attributes.
 */
public final class AgentTurn {

    private final String turnId;
    private final String sessionId;
    private final String userMessage;
    private final Instant startedAt;
    private final Map<String, Object> attributes;
    private final List<ToolCallFingerprint> toolHistory;
    private final Map<String, ToolCallFingerprint> failedFingerprints;

    private int round;
    private boolean softWrapInjected;
    private boolean suspended;
    private String suspensionReason;
    private TerminationReason terminationReason;

    public AgentTurn(String sessionId, String userMessage) {
        this(UUID.randomUUID().toString(), sessionId, userMessage, Instant.now());
    }

    public AgentTurn(String turnId, String sessionId, String userMessage, Instant startedAt) {
        this.turnId = Objects.requireNonNull(turnId, "turnId");
        this.sessionId = sessionId == null ? "default" : sessionId;
        this.userMessage = Objects.requireNonNull(userMessage, "userMessage");
        this.startedAt = startedAt == null ? Instant.now() : startedAt;
        this.attributes = new ConcurrentHashMap<>();
        this.toolHistory = Collections.synchronizedList(new ArrayList<>());
        this.failedFingerprints = new ConcurrentHashMap<>();
        this.round = 0;
    }

    public String turnId() {
        return turnId;
    }

    public String sessionId() {
        return sessionId;
    }

    public String userMessage() {
        return userMessage;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public int round() {
        return round;
    }

    public int nextRound() {
        return ++round;
    }

    public boolean isSoftWrapInjected() {
        return softWrapInjected;
    }

    public void markSoftWrapInjected() {
        this.softWrapInjected = true;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public String suspensionReason() {
        return suspensionReason;
    }

    public void suspend(String reason) {
        this.suspended = true;
        this.suspensionReason = reason;
    }

    public void resume() {
        this.suspended = false;
        this.suspensionReason = null;
    }

    public TerminationReason terminationReason() {
        return terminationReason;
    }

    public void complete(TerminationReason reason) {
        this.terminationReason = reason;
    }

    public Duration elapsed() {
        return Duration.between(startedAt, Instant.now());
    }

    public List<ToolCallFingerprint> toolHistory() {
        synchronized (toolHistory) {
            return List.copyOf(toolHistory);
        }
    }

    public void recordToolCall(ToolCallFingerprint fingerprint) {
        Objects.requireNonNull(fingerprint, "fingerprint");
        toolHistory.add(fingerprint);
        if (!fingerprint.success()) {
            failedFingerprints.put(fingerprint.identityKey(), fingerprint);
        }
        else {
            failedFingerprints.remove(fingerprint.identityKey());
        }
    }

    /**
     * Returns true when the same tool+args fingerprint already failed in this turn.
     */
    public boolean isDuplicateFailedAction(String toolName, String argumentsFingerprint) {
        return failedFingerprints.containsKey(toolName + "|" + argumentsFingerprint);
    }

    public Optional<ToolCallFingerprint> priorFailedAction(String toolName, String argumentsFingerprint) {
        return Optional.ofNullable(failedFingerprints.get(toolName + "|" + argumentsFingerprint));
    }

    public Map<String, Object> attributes() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    public void putAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getAttribute(String key, Class<T> type) {
        Object value = attributes.get(key);
        if (value == null) {
            return Optional.empty();
        }
        if (!type.isInstance(value)) {
            return Optional.empty();
        }
        return Optional.of((T) value);
    }
}
