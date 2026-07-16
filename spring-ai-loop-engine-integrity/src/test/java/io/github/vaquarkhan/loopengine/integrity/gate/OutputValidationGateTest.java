package io.github.vaquarkhan.loopengine.integrity.gate;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OutputValidationGateTest {

    @Test
    void densityGateRejectsEmptyAndLowDensity() {
        OutputValidationGate.DensityGate gate = new OutputValidationGate.DensityGate(0.5);
        assertThat(gate.validate("").valid()).isFalse();
        assertThat(gate.validate("a a a a a a").valid()).isFalse();
        assertThat(gate.validate("alpha beta gamma delta").valid()).isTrue();
    }

    @Test
    void dependencyGateFlagsUnapprovedCoordinates() {
        OutputValidationGate.DependencyGate gate =
                new OutputValidationGate.DependencyGate(Set.of("org.springframework"));
        String content = """
                dependencies {
                  implementation 'org.springframework:spring-core:6.0.0'
                  implementation 'com.evilcorp:malware:1.0.0'
                }
                """;
        OutputValidationGate.ValidationResult result = gate.validate(content);
        assertThat(result.valid()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("com.evilcorp"));
    }

    @Test
    void yamlDesignGateBlocksForbiddenPatterns() {
        OutputValidationGate.YamlDesignGate gate =
                new OutputValidationGate.YamlDesignGate(List.of("(?i)password\\s*:\\s*['\"]?admin"));
        assertThat(gate.validate("password: admin").valid()).isFalse();
        assertThat(gate.validate("password: strong-secret").valid()).isTrue();
    }
}
