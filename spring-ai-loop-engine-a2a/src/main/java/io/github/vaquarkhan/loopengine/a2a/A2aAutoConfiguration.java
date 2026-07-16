package io.github.vaquarkhan.loopengine.a2a;

import io.github.vaquarkhan.loopengine.core.config.LoopEngineProperties;
import io.github.vaquarkhan.loopengine.core.loop.AgentLoopManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(prefix = "spring.ai.loop.a2a", name = "enabled", havingValue = "true", matchIfMissing = true)
public class A2aAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AgentLoopManager.class)
    SubAgentSpawner subAgentSpawner(AgentLoopManager loopManager, LoopEngineProperties properties) {
        return new SubAgentSpawner(loopManager, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication
    AgentCardController agentCardController(LoopEngineProperties properties) {
        return new AgentCardController(properties);
    }
}
