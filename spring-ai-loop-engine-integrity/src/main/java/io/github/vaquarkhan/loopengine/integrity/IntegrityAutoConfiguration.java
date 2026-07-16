package io.github.vaquarkhan.loopengine.integrity;

import io.github.vaquarkhan.loopengine.core.config.LoopEngineProperties;
import io.github.vaquarkhan.loopengine.integrity.gate.OutputValidationGate;
import io.github.vaquarkhan.loopengine.integrity.pvdm.DecisionAttestation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(prefix = "spring.ai.loop.integrity", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IntegrityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    DecisionAttestation.Signer decisionAttestationSigner() {
        return DecisionAttestation.Signer.fromSecret("loop-engine-" + UUID.randomUUID());
    }

    @Bean
    @ConditionalOnMissingBean
    IntegrityLoopListener integrityLoopListener(
            LoopEngineProperties properties,
            DecisionAttestation.Signer signer) {
        List<OutputValidationGate> gates = new ArrayList<>();
        if (properties.getIntegrity().isDensityGateEnabled()) {
            gates.add(new OutputValidationGate.DensityGate(properties.getIntegrity().getMinLogicDensity()));
        }
        if (properties.getIntegrity().isDependencyGateEnabled()) {
            gates.add(new OutputValidationGate.DependencyGate(Set.of(
                    "org.springframework",
                    "io.github.vaquarkhan",
                    "com.fasterxml",
                    "io.projectreactor",
                    "org.apache")));
        }
        gates.add(new OutputValidationGate.YamlDesignGate(List.of(
                "(?i)password\\s*:\\s*['\"]?(admin|root|123456)",
                "(?i)allowPrivilegeEscalation\\s*:\\s*true")));
        return new IntegrityLoopListener(gates, signer, properties.getIntegrity().isPvdmEnabled());
    }
}
