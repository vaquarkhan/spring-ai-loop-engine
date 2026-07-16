package io.github.vaquarkhan.loopengine.a2a;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.vaquarkhan.loopengine.core.config.LoopEngineProperties;
import org.junit.jupiter.api.Test;

class AgentCardControllerTest {

    @Test
    void agentCardIncludesProtocolsAndBudgets() {
        LoopEngineProperties props = new LoopEngineProperties();
        props.setSoftMaxRounds(7);
        props.setHardMaxRounds(11);
        props.getA2a().setAgentName("demo-agent");
        props.getA2a().setAgentDescription("Demo loop");

        AgentCard card = new AgentCardController(props).agentCard();

        assertThat(card.name()).isEqualTo("demo-agent");
        assertThat(card.description()).isEqualTo("Demo loop");
        assertThat(card.capabilities()).contains("loop-engine", "hitl");
        assertThat(card.skills()).isNotEmpty();
        assertThat(card.metadata()).containsEntry("softMaxRounds", 7);
        assertThat(card.metadata()).containsEntry("hardMaxRounds", 11);
        assertThat(card.metadata().get("protocols")).asList().contains("AG-UI", "A2A", "MCP");
    }
}
