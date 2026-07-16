package io.github.vaquarkhan.loopengine.integrity;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.vaquarkhan.loopengine.core.model.AgentTurn;
import io.github.vaquarkhan.loopengine.core.model.LoopResult;
import io.github.vaquarkhan.loopengine.core.model.TerminationReason;
import io.github.vaquarkhan.loopengine.core.model.ToolCallFingerprint;
import io.github.vaquarkhan.loopengine.integrity.gate.OutputValidationGate;
import io.github.vaquarkhan.loopengine.integrity.pvdm.DecisionAttestation;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class IntegrityLoopListenerTest {

    @Test
    void recordsAttestationsAndValidationViolations() {
        DecisionAttestation.Signer signer = DecisionAttestation.Signer.fromSecret("test");
        IntegrityLoopListener listener = new IntegrityLoopListener(
                List.of(new OutputValidationGate.DensityGate(0.9)),
                signer,
                true);

        AgentTurn turn = new AgentTurn("s", "hello");
        listener.onToolCallCompleted(turn, new ToolCallFingerprint(
                "echo", "fp", "{}", true, "ok", Instant.now(), 1));

        LoopResult result = new LoopResult(
                "a a a a a a a a",
                TerminationReason.MODEL_COMPLETION,
                1,
                Duration.ofMillis(1),
                turn);
        listener.onTurnCompleted(result);

        assertThat(listener.attestations()).hasSize(2);
        assertThat(turn.getAttribute("validationViolations", List.class)).isPresent();
        assertThat(turn.getAttribute("completionAttestationId", String.class)).isPresent();
        assertThat(signer.verify(listener.attestations().getFirst())).isTrue();
    }
}
