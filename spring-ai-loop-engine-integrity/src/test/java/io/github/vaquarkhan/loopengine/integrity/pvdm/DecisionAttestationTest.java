package io.github.vaquarkhan.loopengine.integrity.pvdm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DecisionAttestationTest {

    @Test
    void signsAndVerifies() {
        DecisionAttestation.Signer signer = DecisionAttestation.Signer.fromSecret("test-secret");
        DecisionAttestation attestation = signer.attest("turn-1", "TOOL_EXECUTION", "in", "out");
        assertThat(signer.verify(attestation)).isTrue();
        assertThat(attestation.inputDigest()).isNotBlank();
        assertThat(attestation.signature()).hasSize(64);
    }
}
