package io.github.vaquarkhan.loopengine.integrity;

import io.github.vaquarkhan.loopengine.core.loop.LoopListener;
import io.github.vaquarkhan.loopengine.core.loop.ModelRound;
import io.github.vaquarkhan.loopengine.core.model.AgentTurn;
import io.github.vaquarkhan.loopengine.core.model.LoopResult;
import io.github.vaquarkhan.loopengine.core.model.ToolCallFingerprint;
import io.github.vaquarkhan.loopengine.integrity.gate.OutputValidationGate;
import io.github.vaquarkhan.loopengine.integrity.pvdm.DecisionAttestation;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loop listener that validates final outputs and emits PVDM attestations for tool decisions.
 */
public class IntegrityLoopListener implements LoopListener {

    private static final Logger log = LoggerFactory.getLogger(IntegrityLoopListener.class);

    private final List<OutputValidationGate> gates;
    private final DecisionAttestation.Signer signer;
    private final boolean pvdmEnabled;
    private final List<DecisionAttestation> attestations = new CopyOnWriteArrayList<>();

    public IntegrityLoopListener(
            List<OutputValidationGate> gates,
            DecisionAttestation.Signer signer,
            boolean pvdmEnabled) {
        this.gates = gates == null ? List.of() : List.copyOf(gates);
        this.signer = signer;
        this.pvdmEnabled = pvdmEnabled;
    }

    @Override
    public void onToolCallCompleted(AgentTurn turn, ToolCallFingerprint fingerprint) {
        if (!pvdmEnabled || signer == null) {
            return;
        }
        DecisionAttestation attestation = signer.attest(
                turn.turnId(),
                "TOOL_EXECUTION",
                fingerprint.toolName() + "|" + fingerprint.rawArguments(),
                fingerprint.outputSummary());
        attestations.add(attestation);
        turn.putAttribute("lastAttestationId", attestation.attestationId());
        log.debug("PVDM attestation {} for tool {}", attestation.attestationId(), fingerprint.toolName());
    }

    @Override
    public void onTurnCompleted(LoopResult result) {
        List<String> violations = new ArrayList<>();
        for (OutputValidationGate gate : gates) {
            OutputValidationGate.ValidationResult vr = gate.validate(result.content());
            if (!vr.valid()) {
                violations.addAll(vr.violations());
            }
        }
        if (!violations.isEmpty()) {
            result.turn().putAttribute("validationViolations", violations);
            log.warn("Output validation violations on turn {}: {}", result.turn().turnId(), violations);
        }
        if (pvdmEnabled && signer != null) {
            DecisionAttestation attestation = signer.attest(
                    result.turn().turnId(),
                    "LOOP_COMPLETION",
                    result.turn().userMessage(),
                    result.content());
            attestations.add(attestation);
            result.turn().putAttribute("completionAttestationId", attestation.attestationId());
        }
    }

    @Override
    public void onToolCallStarted(AgentTurn turn, ModelRound.ToolInvocation invocation) {
        // no-op
    }

    public List<DecisionAttestation> attestations() {
        return List.copyOf(attestations);
    }
}
