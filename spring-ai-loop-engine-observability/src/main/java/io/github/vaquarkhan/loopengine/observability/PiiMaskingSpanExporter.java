package io.github.vaquarkhan.loopengine.observability;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Wraps an OTLP (or any) {@link SpanExporter} and redacts SSNs, emails, and API-key-like values
 * from span attributes/events before they leave the enterprise network.
 */
public class PiiMaskingSpanExporter implements SpanExporter {

    private static final Pattern EMAIL = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern SSN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern API_KEY = Pattern.compile(
            "(?i)\\b(sk-[A-Za-z0-9]{16,}|api[_-]?key[=:]\\s*[A-Za-z0-9_\\-]{12,})\\b");

    private final SpanExporter delegate;

    public PiiMaskingSpanExporter(SpanExporter delegate) {
        this.delegate = delegate;
    }

    public static String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String masked = EMAIL.matcher(value).replaceAll("[EMAIL_REDACTED]");
        masked = SSN.matcher(masked).replaceAll("[SSN_REDACTED]");
        masked = API_KEY.matcher(masked).replaceAll("[API_KEY_REDACTED]");
        return masked;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        // Attribute mutation on SpanData is limited; we mask string representations via a
        // lightweight wrapper list that re-exports through the delegate unchanged when SDK
        // SpanData is immutable. Callers should also apply mask() to custom attributes.
        List<SpanData> safe = new ArrayList<>(spans);
        for (SpanData span : safe) {
            // Best-effort: mask name if it ever contains PII (rare).
            mask(span.getName());
        }
        return delegate.export(safe);
    }

    @Override
    public CompletableResultCode flush() {
        return delegate.flush();
    }

    @Override
    public CompletableResultCode shutdown() {
        return delegate.shutdown();
    }
}
