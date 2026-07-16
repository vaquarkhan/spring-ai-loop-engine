package io.github.vaquarkhan.loopengine.observability;

import io.github.vaquarkhan.loopengine.core.config.LoopEngineProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(prefix = "spring.ai.loop.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ObservabilityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    GenAiLoopTelemetryListener genAiLoopTelemetryListener(LoopEngineProperties properties) {
        return new GenAiLoopTelemetryListener();
    }
}
