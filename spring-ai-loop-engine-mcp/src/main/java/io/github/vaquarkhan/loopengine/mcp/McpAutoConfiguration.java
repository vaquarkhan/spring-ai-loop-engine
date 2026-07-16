package io.github.vaquarkhan.loopengine.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.vaquarkhan.loopengine.core.config.LoopEngineProperties;
import io.github.vaquarkhan.loopengine.core.tool.ToolExecutor;
import io.github.vaquarkhan.loopengine.mcp.bastion.McpBastionToolExecutor;
import io.github.vaquarkhan.loopengine.mcp.bastion.ToolPermissionEvaluator;
import io.github.vaquarkhan.loopengine.mcp.cursor.CursorMcpConfigGenerator;
import java.nio.file.Path;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(prefix = "spring.ai.loop.mcp", name = "enabled", havingValue = "true", matchIfMissing = true)
public class McpAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(McpAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    ToolPermissionEvaluator toolPermissionEvaluator() {
        return ToolPermissionEvaluator.permitAll();
    }

    @Bean
    @ConditionalOnMissingBean
    CursorMcpConfigGenerator cursorMcpConfigGenerator(ObjectProvider<ObjectMapper> objectMapper) {
        return new CursorMcpConfigGenerator(objectMapper.getIfAvailable(ObjectMapper::new));
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.ai.loop.mcp", name = "bastion-enabled", havingValue = "true", matchIfMissing = true)
    static BeanPostProcessor mcpBastionDecorator(ToolPermissionEvaluator permissionEvaluator) {
        Supplier<String> principalSupplier = () -> {
            try {
                Class<?> holder = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
                Object context = holder.getMethod("getContext").invoke(null);
                Object auth = context.getClass().getMethod("getAuthentication").invoke(context);
                if (auth == null) {
                    return "anonymous";
                }
                Object name = auth.getClass().getMethod("getName").invoke(auth);
                return name == null ? "anonymous" : name.toString();
            }
            catch (ReflectiveOperationException ex) {
                return "anonymous";
            }
        };
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof ToolExecutor executor && !(bean instanceof McpBastionToolExecutor)) {
                    return new McpBastionToolExecutor(executor, permissionEvaluator, principalSupplier);
                }
                return bean;
            }
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.ai.loop.mcp", name = "generate-cursor-config", havingValue = "true", matchIfMissing = true)
    ApplicationRunner cursorMcpJsonWriter(
            CursorMcpConfigGenerator generator,
            LoopEngineProperties properties) {
        return args -> {
            Path path = Path.of(properties.getMcp().getCursorConfigPath());
            String json = generator.generateSse(
                    "spring-ai-loop-engine",
                    "http://localhost:8080/sse");
            generator.writeTo(path, json);
            log.info("Wrote Cursor MCP config to {}", path.toAbsolutePath());
        };
    }
}
