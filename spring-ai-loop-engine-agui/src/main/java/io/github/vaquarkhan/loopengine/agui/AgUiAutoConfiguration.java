package io.github.vaquarkhan.loopengine.agui;

import io.github.vaquarkhan.loopengine.agui.event.AgUiEventBridge;
import io.github.vaquarkhan.loopengine.agui.hitl.HitlApprovalStore;
import io.github.vaquarkhan.loopengine.agui.web.AgUiSseController;
import io.github.vaquarkhan.loopengine.core.config.LoopEngineAutoConfiguration;
import io.github.vaquarkhan.loopengine.core.config.LoopEngineProperties;
import io.github.vaquarkhan.loopengine.core.loop.AgentLoopManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@AutoConfigureAfter(LoopEngineAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(name = "org.springframework.web.reactive.function.server.RouterFunction")
@ConditionalOnProperty(prefix = "spring.ai.loop.agui", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AgUiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    AgUiEventBridge agUiEventBridge() {
        return new AgUiEventBridge();
    }

    @Bean
    @ConditionalOnMissingBean
    HitlApprovalStore hitlApprovalStore() {
        return new HitlApprovalStore();
    }

    @Bean
    @ConditionalOnMissingBean
    AgUiSseController agUiSseController(
            AgentLoopManager loopManager,
            AgUiEventBridge eventBridge,
            HitlApprovalStore approvalStore,
            LoopEngineProperties properties) {
        return new AgUiSseController(loopManager, eventBridge, approvalStore, properties);
    }
}
