# spring-ai-loop-engine-integrity

Output validation gates and PVDM decision attestation for safe loop termination.

Enterprises need assurance that autonomous loops do not emit low-quality output or commit changes without an audit trail. This module validates final content and cryptographically binds tool/loop decisions to verified inputs.

## Artifact

```xml
<dependency>
  <groupId>io.github.vaquarkhan</groupId>
  <artifactId>spring-ai-loop-engine-integrity</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Package layout

| Package / class | Role |
|-----------------|------|
| `gate.OutputValidationGate` | Gate SPI + density / dependency / YAML design implementations |
| `pvdm.DecisionAttestation` | HMAC-signed attestation record + `Signer` |
| `IntegrityLoopListener` | `LoopListener` that runs gates and emits attestations |
| `IntegrityAutoConfiguration` | Spring Boot wiring |

## Gates

| Gate | What it checks |
|------|----------------|
| **DensityGate** | Logic density / uniqueness ratio vs `min-logic-density` |
| **DependencyGate** | Flags unapproved Maven/Gradle coordinates in generated text |
| **YamlDesignGate** | Blocks forbidden design patterns (e.g. weak passwords, privilege escalation) |

Violations are recorded on the `AgentTurn` attributes (`validationViolations`) and logged.

## PVDM-A attestation

Every tool execution and loop completion can produce a signed attestation:

```java
DecisionAttestation.Signer signer = DecisionAttestation.Signer.fromSecret("change-me");
DecisionAttestation a = signer.attest(turnId, "TOOL_EXECUTION", input, output);
boolean ok = signer.verify(a);
```

Fields: `attestationId`, `turnId`, `decisionType`, `inputDigest`, `outputDigest`, `createdAt`, `signature`.

## Configuration

| Property | Default |
|----------|---------|
| `spring.ai.loop.integrity.enabled` | `true` |
| `spring.ai.loop.integrity.pvdm-enabled` | `true` |
| `spring.ai.loop.integrity.density-gate-enabled` | `true` |
| `spring.ai.loop.integrity.dependency-gate-enabled` | `true` |
| `spring.ai.loop.integrity.min-logic-density` | `0.15` |

## Tests

```bash
mvn -pl spring-ai-loop-engine-integrity test
```
