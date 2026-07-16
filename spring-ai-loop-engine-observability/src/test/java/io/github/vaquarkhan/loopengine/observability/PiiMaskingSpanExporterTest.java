package io.github.vaquarkhan.loopengine.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PiiMaskingSpanExporterTest {

    @Test
    void masksEmailSsnAndApiKey() {
        String input = "user jane.doe@example.com ssn 123-45-6789 key sk-abcdefghijklmnopqrstuvwxyz";
        String masked = PiiMaskingSpanExporter.mask(input);
        assertThat(masked).contains("[EMAIL_REDACTED]", "[SSN_REDACTED]", "[API_KEY_REDACTED]");
        assertThat(masked).doesNotContain("jane.doe@example.com", "123-45-6789", "sk-abcdefghijklmnopqrstuvwxyz");
    }
}
