package io.github.vaquarkhan.loopengine.mcp.bastion;

import java.util.Set;

/**
 * Resolves which tools the invoking human principal is allowed to run.
 */
@FunctionalInterface
public interface ToolPermissionEvaluator {

    boolean isAllowed(String principal, String toolName);

    /**
     * Allow-list evaluator keyed by principal name.
     */
    static ToolPermissionEvaluator allowList(java.util.Map<String, Set<String>> grants) {
        return (principal, toolName) -> {
            if (principal == null) {
                return false;
            }
            Set<String> tools = grants.get(principal);
            return tools != null && (tools.contains("*") || tools.contains(toolName));
        };
    }

    static ToolPermissionEvaluator permitAll() {
        return (principal, toolName) -> true;
    }
}
