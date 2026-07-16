package io.github.vaquarkhan.loopengine.agui;

import io.github.vaquarkhan.loopengine.agui.event.AgUiEventBridge;
import io.github.vaquarkhan.loopengine.agui.hitl.HitlApprovalStore;
import io.github.vaquarkhan.loopengine.agui.web.AgUiSseController;
import io.github.vaquarkhan.loopengine.core.config.LoopEngineProperties;
import io.github.vaquarkhan.loopengine.core.loop.AgentLoopManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(name = "org.springframework.web.reactive.function.server.RouterFunction")
@ConditionalOnProperty(prefix = "spring.ai.loop.agui", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({AgUiEventBridge.class, HitlApprovalStore.class})
public class AgUiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AgentLoopManager.class)
    AgUiSseController agUiSseController(
            AgentLoopManager loopManager,
            AgUiEventBridge eventBridge,
            HitlApprovalStore approvalStore,
            LoopEngineProperties properties) {
        return new AgUiSseController(loopManager, eventBridge, approvalStore, properties);
    }
}
