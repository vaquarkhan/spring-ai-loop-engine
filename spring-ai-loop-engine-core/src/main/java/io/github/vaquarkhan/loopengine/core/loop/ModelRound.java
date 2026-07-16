package io.github.vaquarkhan.loopengine.core.loop;

import java.util.List;
import java.util.Objects;

/**
 * One model turn within the agent loop: either a final answer or tool call requests.
 */
public final class ModelRound {

    private final String content;
    private final List<ToolInvocation> toolInvocations;
    private final Long inputTokens;
    private final Long outputTokens;
    private final String finishReason;

    public ModelRound(
            String content,
            List<ToolInvocation> toolInvocations,
            Long inputTokens,
            Long outputTokens,
            String finishReason) {
        this.content = content == null ? "" : content;
        this.toolInvocations = toolInvocations == null ? List.of() : List.copyOf(toolInvocations);
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.finishReason = finishReason;
    }

    public static ModelRound finalAnswer(String content) {
        return new ModelRound(content, List.of(), null, null, "stop");
    }

    public static ModelRound tools(List<ToolInvocation> tools) {
        return new ModelRound("", tools, null, null, "tool_calls");
    }

    public String content() {
        return content;
    }

    public List<ToolInvocation> toolInvocations() {
        return toolInvocations;
    }

    public boolean hasToolCalls() {
        return !toolInvocations.isEmpty();
    }

    public Long inputTokens() {
        return inputTokens;
    }

    public Long outputTokens() {
        return outputTokens;
    }

    public String finishReason() {
        return finishReason;
    }

    /**
     * A single tool call requested by the model.
     */
    public record ToolInvocation(String id, String toolName, String arguments) {
        public ToolInvocation {
            Objects.requireNonNull(toolName, "toolName");
            arguments = arguments == null ? "{}" : arguments;
            id = id == null ? toolName : id;
        }
    }
}
