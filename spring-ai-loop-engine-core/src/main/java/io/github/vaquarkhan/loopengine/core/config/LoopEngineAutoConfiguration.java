package io.github.vaquarkhan.loopengine.core.config;

import io.github.vaquarkhan.loopengine.core.loop.AgentLoopManager;
import io.github.vaquarkhan.loopengine.core.loop.ChatClientLoopModelClient;
import io.github.vaquarkhan.loopengine.core.loop.LoopListener;
import io.github.vaquarkhan.loopengine.core.loop.LoopModelClient;
import io.github.vaquarkhan.loopengine.core.tool.ToolCallbackToolExecutor;
import io.github.vaquarkhan.loopengine.core.tool.ToolExecutor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Zero-configuration auto-wiring for {@link AgentLoopManager}.
 */
@AutoConfiguration
@EnableConfigurationProperties(LoopEngineProperties.class)
@ConditionalOnProperty(prefix = "spring.ai.loop", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LoopEngineAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(ChatClient.class)
    @ConditionalOnBean(ChatClient.Builder.class)
    LoopModelClient loopModelClient(ChatClient.Builder chatClientBuilder) {
        return new ChatClientLoopModelClient(chatClientBuilder.build(), false);
    }

    @Bean
    @ConditionalOnMissingBean
    ToolExecutor toolExecutor(ObjectProvider<ToolCallbackProvider> providers) {
        ToolCallbackProvider[] array = providers.orderedStream().toArray(ToolCallbackProvider[]::new);
        return new ToolCallbackToolExecutor(array);
    }

    @Bean
    @ConditionalOnMissingBean
    AgentLoopManager agentLoopManager(
            LoopModelClient loopModelClient,
            ToolExecutor toolExecutor,
            LoopEngineProperties properties,
            ObjectProvider<LoopListener> listeners) {
        return new AgentLoopManager(
                loopModelClient,
                toolExecutor,
                properties,
                () -> listeners.orderedStream().toList());
    }
}
