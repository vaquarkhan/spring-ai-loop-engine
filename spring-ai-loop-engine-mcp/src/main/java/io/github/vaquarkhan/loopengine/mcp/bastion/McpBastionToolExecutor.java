package io.github.vaquarkhan.loopengine.mcp.bastion;

import io.github.vaquarkhan.loopengine.core.loop.ModelRound;
import io.github.vaquarkhan.loopengine.core.model.AgentTurn;
import io.github.vaquarkhan.loopengine.core.tool.ToolExecutor;
import java.util.Objects;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Zero-trust MCP Bastion: sits between the agent loop and external tools,
 * enforcing RBAC so the loop only executes tools the invoking human may use.
 */
public class McpBastionToolExecutor implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(McpBastionToolExecutor.class);

    private final ToolExecutor delegate;
    private final ToolPermissionEvaluator permissionEvaluator;
    private final Supplier<String> principalSupplier;

    public McpBastionToolExecutor(
            ToolExecutor delegate,
            ToolPermissionEvaluator permissionEvaluator,
            Supplier<String> principalSupplier) {
        this.delegate = Objects.requireNonNull(delegate);
        this.permissionEvaluator = Objects.requireNonNull(permissionEvaluator);
        this.principalSupplier = principalSupplier == null ? () -> "anonymous" : principalSupplier;
    }

    @Override
    public ToolResult execute(AgentTurn turn, ModelRound.ToolInvocation invocation) {
        String principal = principalSupplier.get();
        if (!permissionEvaluator.isAllowed(principal, invocation.toolName())) {
            log.warn("MCP Bastion denied tool '{}' for principal '{}' on turn {}",
                    invocation.toolName(), principal, turn.turnId());
            return ToolResult.failure("{\"error\":\"rbac_denied\",\"tool\":\""
                    + invocation.toolName() + "\",\"principal\":\"" + principal + "\"}");
        }
        return delegate.execute(turn, invocation);
    }
}
