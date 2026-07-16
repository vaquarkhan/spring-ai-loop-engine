package io.github.vaquarkhan.loopengine.a2a;

import java.util.List;
import java.util.Map;

/**
 * Agent discovery card exposed at {@code /.well-known/agent-card.json}.
 */
public record AgentCard(
        String name,
        String description,
        String version,
        String url,
        List<String> capabilities,
        List<Skill> skills,
        Map<String, Object> metadata) {

    public record Skill(String id, String name, String description, List<String> tags) {
    }
}
