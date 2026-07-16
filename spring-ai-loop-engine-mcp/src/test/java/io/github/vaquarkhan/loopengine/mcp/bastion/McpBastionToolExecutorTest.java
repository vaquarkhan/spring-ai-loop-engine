package io.github.vaquarkhan.loopengine.mcp.bastion;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.vaquarkhan.loopengine.core.loop.ModelRound;
import io.github.vaquarkhan.loopengine.core.model.AgentTurn;
import io.github.vaquarkhan.loopengine.core.tool.ToolExecutor;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class McpBastionToolExecutorTest {

    @Test
    void deniesWhenPrincipalLacksPermission() {
        ToolExecutor delegate = (turn, inv) -> ToolExecutor.ToolResult.ok("should-not-run");
        McpBastionToolExecutor bastion = new McpBastionToolExecutor(
                delegate,
                ToolPermissionEvaluator.allowList(Map.of("alice", Set.of("safe"))),
                () -> "alice");

        ToolExecutor.ToolResult result = bastion.execute(
                new AgentTurn("s", "hi"),
                new ModelRound.ToolInvocation("1", "danger", "{}"));

        assertThat(result.success()).isFalse();
        assertThat(result.output()).contains("rbac_denied");
    }

    @Test
    void allowsWildcardAndDelegates() {
        AtomicBoolean called = new AtomicBoolean();
        ToolExecutor delegate = (turn, inv) -> {
            called.set(true);
            return ToolExecutor.ToolResult.ok("{\"ok\":true}");
        };
        McpBastionToolExecutor bastion = new McpBastionToolExecutor(
                delegate,
                ToolPermissionEvaluator.allowList(Map.of("admin", Set.of("*"))),
                () -> "admin");

        ToolExecutor.ToolResult result = bastion.execute(
                new AgentTurn("s", "hi"),
                new ModelRound.ToolInvocation("1", "anything", "{}"));

        assertThat(called).isTrue();
        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("ok");
    }

    @Test
    void permitAllAllowsAnyPrincipal() {
        McpBastionToolExecutor bastion = new McpBastionToolExecutor(
                (t, i) -> ToolExecutor.ToolResult.ok("x"),
                ToolPermissionEvaluator.permitAll(),
                () -> "anonymous");

        assertThat(bastion.execute(
                        new AgentTurn("s", "hi"),
                        new ModelRound.ToolInvocation("1", "echo", "{}"))
                .success()).isTrue();
    }
}
