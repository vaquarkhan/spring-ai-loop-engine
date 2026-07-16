package io.github.vaquarkhan.loopengine.integrity.gate;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates agent final output before the loop terminates.
 */
public interface OutputValidationGate {

    ValidationResult validate(String content);

    record ValidationResult(boolean valid, List<String> violations) {
        public static ValidationResult ok() {
            return new ValidationResult(true, List.of());
        }

        public static ValidationResult fail(String... violations) {
            return new ValidationResult(false, List.of(violations));
        }
    }

    /**
     * Logic density gate: rejects low-signal, high-verbosity boilerplate.
     */
    class DensityGate implements OutputValidationGate {
        private final double minDensity;

        public DensityGate(double minDensity) {
            this.minDensity = minDensity;
        }

        @Override
        public ValidationResult validate(String content) {
            if (content == null || content.isBlank()) {
                return ValidationResult.fail("empty_output");
            }
            String[] tokens = content.trim().split("\\s+");
            long unique = java.util.Arrays.stream(tokens).map(String::toLowerCase).distinct().count();
            double density = tokens.length == 0 ? 0.0 : (double) unique / tokens.length;
            if (density < minDensity) {
                return ValidationResult.fail("logic_density_too_low:" + String.format("%.3f", density));
            }
            return ValidationResult.ok();
        }
    }

    /**
     * Dependency gate: flags hallucinated Maven/Gradle coordinates that look invented.
     */
    class DependencyGate implements OutputValidationGate {
        private static final Pattern COORD = Pattern.compile(
                "(?:implementation|compile|api)\\s*[(\\['\"]\\s*([\\w.-]+):([\\w.-]+):([\\w.-]+)");

        private final java.util.Set<String> allowedGroupPrefixes;

        public DependencyGate(java.util.Set<String> allowedGroupPrefixes) {
            this.allowedGroupPrefixes = allowedGroupPrefixes;
        }

        @Override
        public ValidationResult validate(String content) {
            if (content == null || content.isBlank()) {
                return ValidationResult.ok();
            }
            List<String> violations = new ArrayList<>();
            Matcher matcher = COORD.matcher(content);
            while (matcher.find()) {
                String group = matcher.group(1);
                boolean allowed = allowedGroupPrefixes.stream().anyMatch(group::startsWith);
                if (!allowed) {
                    violations.add("unapproved_dependency:" + group + ":" + matcher.group(2));
                }
            }
            return violations.isEmpty() ? ValidationResult.ok() : new ValidationResult(false, violations);
        }
    }

    /**
     * YAML design rule gate: blocks obviously forbidden patterns.
     */
    class YamlDesignGate implements OutputValidationGate {
        private final List<Pattern> forbidden;

        public YamlDesignGate(List<String> forbiddenPatterns) {
            this.forbidden = forbiddenPatterns.stream().map(Pattern::compile).toList();
        }

        @Override
        public ValidationResult validate(String content) {
            if (content == null) {
                return ValidationResult.ok();
            }
            List<String> violations = new ArrayList<>();
            for (Pattern pattern : forbidden) {
                if (pattern.matcher(content).find()) {
                    violations.add("yaml_design_violation:" + pattern.pattern());
                }
            }
            return violations.isEmpty() ? ValidationResult.ok() : new ValidationResult(false, violations);
        }
    }
}
