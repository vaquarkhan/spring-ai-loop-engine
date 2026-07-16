package io.github.vaquarkhan.loopengine.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.vaquarkhan.loopengine.core.loop.ModelRound;
import io.github.vaquarkhan.loopengine.core.model.AgentTurn;
import io.github.vaquarkhan.loopengine.core.model.LoopResult;
import io.github.vaquarkhan.loopengine.core.model.TerminationReason;
import io.github.vaquarkhan.loopengine.core.model.ToolCallFingerprint;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class GenAiLoopTelemetryListenerTest {

    @Test
    void recordsTurnAndToolSpans() {
        InMemorySpanExporter exporter = InMemorySpanExporter.create();
        SdkTracerProvider provider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        Tracer tracer = provider.get("test");

        GenAiLoopTelemetryListener listener = new GenAiLoopTelemetryListener(tracer);
        AgentTurn turn = new AgentTurn("sess", "observe me");

        listener.onTurnStarted(turn);
        listener.onRoundStarted(turn, 1);
        listener.onToolCallStarted(turn, new ModelRound.ToolInvocation("1", "echo", "{}"));
        listener.onToolCallCompleted(turn, new ToolCallFingerprint(
                "echo", "fp", "{}", true, "ok", Instant.now(), 1));
        listener.onTurnCompleted(new LoopResult(
                "done", TerminationReason.MODEL_COMPLETION, 1, Duration.ofMillis(12), turn));

        provider.forceFlush();
        assertThat(exporter.getFinishedSpanItems()).isNotEmpty();
        assertThat(exporter.getFinishedSpanItems().getFirst().getName()).isEqualTo("agent.loop.turn");
        assertThat(exporter.getFinishedSpanItems().getFirst().getAttributes()
                .get(io.opentelemetry.api.common.AttributeKey.stringKey("agent.termination_reason")))
                .isEqualTo("MODEL_COMPLETION");
    }
}
