package io.github.vaquarkhan.loopengine.core.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for the Loop Engine ({@code spring.ai.loop.*}).
 */
@ConfigurationProperties(prefix = "spring.ai.loop")
@Validated
public class LoopEngineProperties {

    /**
     * Master enable switch for auto-configuration.
     */
    private boolean enabled = true;

    /**
     * Soft max rounds: inject wrap-up prompt and stop further tool calls.
     */
    @Min(1)
    private int softMaxRounds = 15;

    /**
     * Hard max rounds: force stop with {@code HardMaxRoundsExceededException}.
     */
    @Min(1)
    private int hardMaxRounds = 25;

    /**
     * Block exact tool+args fingerprints that already failed in the current turn.
     */
    private boolean blockDuplicateFailedActions = true;

    /**
     * Default system prompt used when none is supplied on the request.
     */
    private String systemPrompt = "You are a careful autonomous agent. Prefer tools when needed, "
            + "learn from tool errors, and never retry the exact same failed action.";

    private final Agui agui = new Agui();
    private final A2a a2a = new A2a();
    private final Mcp mcp = new Mcp();
    private final Integrity integrity = new Integrity();
    private final Observability observability = new Observability();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getSoftMaxRounds() {
        return softMaxRounds;
    }

    public void setSoftMaxRounds(int softMaxRounds) {
        this.softMaxRounds = softMaxRounds;
    }

    public int getHardMaxRounds() {
        return hardMaxRounds;
    }

    public void setHardMaxRounds(int hardMaxRounds) {
        this.hardMaxRounds = hardMaxRounds;
    }

    public boolean isBlockDuplicateFailedActions() {
        return blockDuplicateFailedActions;
    }

    public void setBlockDuplicateFailedActions(boolean blockDuplicateFailedActions) {
        this.blockDuplicateFailedActions = blockDuplicateFailedActions;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public Agui getAgui() {
        return agui;
    }

    public A2a getA2a() {
        return a2a;
    }

    public Mcp getMcp() {
        return mcp;
    }

    public Integrity getIntegrity() {
        return integrity;
    }

    public Observability getObservability() {
        return observability;
    }

    public static class Agui {
        private boolean enabled = true;
        private String ssePath = "/api/loop/ag-ui";
        private String approvalPath = "/api/loop/approvals";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSsePath() {
            return ssePath;
        }

        public void setSsePath(String ssePath) {
            this.ssePath = ssePath;
        }

        public String getApprovalPath() {
            return approvalPath;
        }

        public void setApprovalPath(String approvalPath) {
            this.approvalPath = approvalPath;
        }
    }

    public static class A2a {
        private boolean enabled = true;
        private String agentCardPath = "/.well-known/agent-card.json";
        private String agentName = "spring-ai-loop-engine";
        private String agentDescription = "Stateful Spring AI agent loop with soft/hard round bounds";
        private int defaultSubAgentTokenBudget = 8000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getAgentCardPath() {
            return agentCardPath;
        }

        public void setAgentCardPath(String agentCardPath) {
            this.agentCardPath = agentCardPath;
        }

        public String getAgentName() {
            return agentName;
        }

        public void setAgentName(String agentName) {
            this.agentName = agentName;
        }

        public String getAgentDescription() {
            return agentDescription;
        }

        public void setAgentDescription(String agentDescription) {
            this.agentDescription = agentDescription;
        }

        public int getDefaultSubAgentTokenBudget() {
            return defaultSubAgentTokenBudget;
        }

        public void setDefaultSubAgentTokenBudget(int defaultSubAgentTokenBudget) {
            this.defaultSubAgentTokenBudget = defaultSubAgentTokenBudget;
        }
    }

    public static class Mcp {
        private boolean enabled = true;
        private boolean bastionEnabled = true;
        private boolean generateCursorConfig = true;
        private String cursorConfigPath = ".cursor/mcp.json";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isBastionEnabled() {
            return bastionEnabled;
        }

        public void setBastionEnabled(boolean bastionEnabled) {
            this.bastionEnabled = bastionEnabled;
        }

        public boolean isGenerateCursorConfig() {
            return generateCursorConfig;
        }

        public void setGenerateCursorConfig(boolean generateCursorConfig) {
            this.generateCursorConfig = generateCursorConfig;
        }

        public String getCursorConfigPath() {
            return cursorConfigPath;
        }

        public void setCursorConfigPath(String cursorConfigPath) {
            this.cursorConfigPath = cursorConfigPath;
        }
    }

    public static class Integrity {
        private boolean enabled = true;
        private boolean pvdmEnabled = true;
        private boolean densityGateEnabled = true;
        private boolean dependencyGateEnabled = true;
        private double minLogicDensity = 0.15;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isPvdmEnabled() {
            return pvdmEnabled;
        }

        public void setPvdmEnabled(boolean pvdmEnabled) {
            this.pvdmEnabled = pvdmEnabled;
        }

        public boolean isDensityGateEnabled() {
            return densityGateEnabled;
        }

        public void setDensityGateEnabled(boolean densityGateEnabled) {
            this.densityGateEnabled = densityGateEnabled;
        }

        public boolean isDependencyGateEnabled() {
            return dependencyGateEnabled;
        }

        public void setDependencyGateEnabled(boolean dependencyGateEnabled) {
            this.dependencyGateEnabled = dependencyGateEnabled;
        }

        public double getMinLogicDensity() {
            return minLogicDensity;
        }

        public void setMinLogicDensity(double minLogicDensity) {
            this.minLogicDensity = minLogicDensity;
        }
    }

    public static class Observability {
        private boolean enabled = true;
        private boolean piiMaskingEnabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isPiiMaskingEnabled() {
            return piiMaskingEnabled;
        }

        public void setPiiMaskingEnabled(boolean piiMaskingEnabled) {
            this.piiMaskingEnabled = piiMaskingEnabled;
        }
    }
}
