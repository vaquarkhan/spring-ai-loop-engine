package io.github.vaquarkhan.loopengine.core.tool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ArgumentFingerprinterTest {

    @Test
    void sameLogicalArgsProduceSameFingerprint() {
        String a = ArgumentFingerprinter.fingerprint("echo", "{ \"x\": 1 }");
        String b = ArgumentFingerprinter.fingerprint("echo", "{\"x\": 1}");
        assertThat(a).isEqualTo(b);
    }

    @Test
    void differentArgsDiffer() {
        String a = ArgumentFingerprinter.fingerprint("echo", "{\"x\":1}");
        String b = ArgumentFingerprinter.fingerprint("echo", "{\"x\":2}");
        assertThat(a).isNotEqualTo(b);
    }
}
