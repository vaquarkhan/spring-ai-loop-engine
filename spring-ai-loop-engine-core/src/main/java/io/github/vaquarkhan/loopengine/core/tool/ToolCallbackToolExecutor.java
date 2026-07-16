package io.github.vaquarkhan.loopengine.core.tool;

import io.github.vaquarkhan.loopengine.core.loop.ModelRound;
import io.github.vaquarkhan.loopengine.core.model.AgentTurn;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

/**
 * {@link ToolExecutor} that discovers Spring AI {@link ToolCallback} beans
 * (including MCP and AgentCore providers) and executes them by name.
 */
public class ToolCallbackToolExecutor implements ToolExecutor {

    private final Map<String, ToolCallback> callbacks;

    public ToolCallbackToolExecutor(ToolCallback... callbacks) {
        this.callbacks = Arrays.stream(callbacks)
                .filter(Objects::nonNull)
                .collect(Collectors.toConcurrentMap(
                        cb -> cb.getToolDefinition().name(),
                        Function.identity(),
                        (a, b) -> b,
                        ConcurrentHashMap::new));
    }

    public ToolCallbackToolExecutor(ToolCallbackProvider... providers) {
        this.callbacks = new ConcurrentHashMap<>();
        if (providers != null) {
            for (ToolCallbackProvider provider : providers) {
                if (provider == null) {
                    continue;
                }
                ToolCallback[] tools = provider.getToolCallbacks();
                if (tools == null) {
                    continue;
                }
                for (ToolCallback callback : tools) {
                    callbacks.put(callback.getToolDefinition().name(), callback);
                }
            }
        }
    }

    public void register(ToolCallback callback) {
        callbacks.put(callback.getToolDefinition().name(), callback);
    }

    @Override
    public ToolResult execute(AgentTurn turn, ModelRound.ToolInvocation invocation) {
        ToolCallback callback = callbacks.get(invocation.toolName());
        if (callback == null) {
            return ToolResult.failure("{\"error\":\"unknown_tool\",\"tool\":\""
                    + invocation.toolName() + "\"}");
        }
        try {
            String output = callback.call(invocation.arguments());
            return ToolResult.ok(output);
        }
        catch (Exception ex) {
            return ToolResult.failure("{\"error\":\"tool_exception\",\"message\":\""
                    + sanitize(ex.getMessage()) + "\"}");
        }
    }

    private static String sanitize(String message) {
        if (message == null) {
            return "unknown";
        }
        return message.replace("\"", "'").replace("\n", " ");
    }
}
