package io.github.vaquarkhan.loopengine.integrity.pvdm;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * PVDM-A decision attestation: cryptographically binds an agent decision to verified inputs.
 */
public record DecisionAttestation(
        String attestationId,
        String turnId,
        String decisionType,
        String inputDigest,
        String outputDigest,
        Instant createdAt,
        String signature) {

    public static final class Signer {
        private final byte[] hmacKey;

        public Signer(byte[] hmacKey) {
            this.hmacKey = Objects.requireNonNull(hmacKey, "hmacKey").clone();
        }

        public static Signer fromSecret(String secret) {
            return new Signer(secret.getBytes(StandardCharsets.UTF_8));
        }

        public DecisionAttestation attest(String turnId, String decisionType, String input, String output) {
            String inputDigest = sha256(input == null ? "" : input);
            String outputDigest = sha256(output == null ? "" : output);
            Instant now = Instant.now();
            String attestationId = UUID.randomUUID().toString();
            String payload = attestationId + "|" + turnId + "|" + decisionType + "|"
                    + inputDigest + "|" + outputDigest + "|" + now;
            String signature = hmac(payload);
            return new DecisionAttestation(
                    attestationId, turnId, decisionType, inputDigest, outputDigest, now, signature);
        }

        public boolean verify(DecisionAttestation attestation) {
            String payload = attestation.attestationId() + "|" + attestation.turnId() + "|"
                    + attestation.decisionType() + "|" + attestation.inputDigest() + "|"
                    + attestation.outputDigest() + "|" + attestation.createdAt();
            return MessageDigest.isEqual(
                    attestation.signature().getBytes(StandardCharsets.UTF_8),
                    hmac(payload).getBytes(StandardCharsets.UTF_8));
        }

        private String hmac(String payload) {
            try {
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
                return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            }
            catch (Exception e) {
                throw new IllegalStateException("Unable to sign attestation", e);
            }
        }

        private static String sha256(String value) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
            }
            catch (Exception e) {
                throw new IllegalStateException("SHA-256 unavailable", e);
            }
        }
    }
}
