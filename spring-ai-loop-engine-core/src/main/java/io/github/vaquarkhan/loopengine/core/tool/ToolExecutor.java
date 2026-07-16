package io.github.vaquarkhan.loopengine.core.tool;

import io.github.vaquarkhan.loopengine.core.loop.ModelRound;
import io.github.vaquarkhan.loopengine.core.model.AgentTurn;

/**
 * Executes a single tool invocation. Implementations may wrap Spring AI
 * {@code ToolCallingManager}, {@code ToolCallback}, or MCP tool bridges.
 */
@FunctionalInterface
public interface ToolExecutor {

    ToolResult execute(AgentTurn turn, ModelRound.ToolInvocation invocation);

    record ToolResult(boolean success, String output) {
        public static ToolResult ok(String output) {
            return new ToolResult(true, output == null ? "" : output);
        }

        public static ToolResult failure(String output) {
            return new ToolResult(false, output == null ? "{\"error\":\"tool_failed\"}" : output);
        }
    }
}
