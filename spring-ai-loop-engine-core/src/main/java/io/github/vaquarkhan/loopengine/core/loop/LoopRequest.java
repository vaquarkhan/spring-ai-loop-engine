package io.github.vaquarkhan.loopengine.core.loop;

import io.github.vaquarkhan.loopengine.core.model.AgentTurn;
import java.util.List;
import java.util.Objects;

/**
 * Request to run one agent turn through the loop engine.
 */
public final class LoopRequest {

    private final String sessionId;
    private final String userMessage;
    private final String systemPrompt;
    private final List<String> conversationHistory;
    private final AgentTurn existingTurn;
    private final Integer softMaxRoundsOverride;
    private final Integer hardMaxRoundsOverride;

    public LoopRequest(String sessionId, String userMessage, String systemPrompt, List<String> conversationHistory) {
        this(sessionId, userMessage, systemPrompt, conversationHistory, null, null, null);
    }

    public LoopRequest(
            String sessionId,
            String userMessage,
            String systemPrompt,
            List<String> conversationHistory,
            AgentTurn existingTurn) {
        this(sessionId, userMessage, systemPrompt, conversationHistory, existingTurn, null, null);
    }

    public LoopRequest(
            String sessionId,
            String userMessage,
            String systemPrompt,
            List<String> conversationHistory,
            AgentTurn existingTurn,
            Integer softMaxRoundsOverride,
            Integer hardMaxRoundsOverride) {
        this.sessionId = sessionId == null ? "default" : sessionId;
        this.userMessage = Objects.requireNonNull(userMessage, "userMessage");
        this.systemPrompt = systemPrompt == null ? "" : systemPrompt;
        this.conversationHistory = conversationHistory == null ? List.of() : List.copyOf(conversationHistory);
        this.existingTurn = existingTurn;
        this.softMaxRoundsOverride = softMaxRoundsOverride;
        this.hardMaxRoundsOverride = hardMaxRoundsOverride;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String sessionId() {
        return sessionId;
    }

    public String userMessage() {
        return userMessage;
    }

    public String systemPrompt() {
        return systemPrompt;
    }

    public List<String> conversationHistory() {
        return conversationHistory;
    }

    public Integer softMaxRoundsOverride() {
        return softMaxRoundsOverride;
    }

    public Integer hardMaxRoundsOverride() {
        return hardMaxRoundsOverride;
    }

    public AgentTurn newTurn() {
        return existingTurn != null ? existingTurn : new AgentTurn(sessionId, userMessage);
    }

    public static final class Builder {
        private String sessionId = "default";
        private String userMessage;
        private String systemPrompt = "";
        private List<String> conversationHistory = List.of();
        private AgentTurn existingTurn;
        private Integer softMaxRoundsOverride;
        private Integer hardMaxRoundsOverride;

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder userMessage(String userMessage) {
            this.userMessage = userMessage;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder conversationHistory(List<String> conversationHistory) {
            this.conversationHistory = conversationHistory;
            return this;
        }

        public Builder turn(AgentTurn turn) {
            this.existingTurn = turn;
            return this;
        }

        public Builder softMaxRounds(int softMaxRounds) {
            this.softMaxRoundsOverride = softMaxRounds;
            return this;
        }

        public Builder hardMaxRounds(int hardMaxRounds) {
            this.hardMaxRoundsOverride = hardMaxRounds;
            return this;
        }

        public LoopRequest build() {
            return new LoopRequest(
                    sessionId,
                    userMessage,
                    systemPrompt,
                    conversationHistory,
                    existingTurn,
                    softMaxRoundsOverride,
                    hardMaxRoundsOverride);
        }
    }
}
