package io.github.vaquarkhan.loopengine.core.tool;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Stable fingerprinting of tool arguments to detect duplicate failed retries.
 */
public final class ArgumentFingerprinter {

    private ArgumentFingerprinter() {
    }

    public static String fingerprint(String toolName, String arguments) {
        String normalized = normalize(arguments);
        return sha256(toolName.toLowerCase(Locale.ROOT) + "::" + normalized);
    }

    static String normalize(String arguments) {
        if (arguments == null) {
            return "";
        }
        String trimmed = arguments.trim();
        // Collapse whitespace and remove insignificant JSON spacing for stable hashing.
        return trimmed
                .replaceAll("\\s+", "")
                .replaceAll(",}", "}")
                .replaceAll(",]", "]");
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
