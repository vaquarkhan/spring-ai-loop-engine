package io.github.vaquarkhan.loopengine.observability;

import io.github.vaquarkhan.loopengine.core.loop.LoopListener;
import io.github.vaquarkhan.loopengine.core.loop.ModelRound;
import io.github.vaquarkhan.loopengine.core.model.AgentTurn;
import io.github.vaquarkhan.loopengine.core.model.LoopResult;
import io.github.vaquarkhan.loopengine.core.model.ToolCallFingerprint;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Emits GenAI semantic spans for agent loop turns and tool calls.
 */
public class GenAiLoopTelemetryListener implements LoopListener {

    public static final AttributeKey<Long> INPUT_TOKENS = AttributeKey.longKey("gen_ai.usage.input_tokens");
    public static final AttributeKey<Long> OUTPUT_TOKENS = AttributeKey.longKey("gen_ai.usage.output_tokens");
    public static final AttributeKey<String> FINISH_REASON = AttributeKey.stringKey("gen_ai.response.finish_reasons");
    public static final AttributeKey<String> OPERATION = AttributeKey.stringKey("gen_ai.operation.name");
    public static final AttributeKey<String> AGENT_TURN = AttributeKey.stringKey("agent.turn.id");

    private final Tracer tracer;
    private final Map<String, Span> turnSpans = new ConcurrentHashMap<>();
    private final Map<String, Scope> turnScopes = new ConcurrentHashMap<>();

    public GenAiLoopTelemetryListener() {
        this(GlobalOpenTelemetry.getTracer("spring-ai-loop-engine"));
    }

    public GenAiLoopTelemetryListener(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void onTurnStarted(AgentTurn turn) {
        Span span = tracer.spanBuilder("agent.loop.turn")
                .setAttribute(OPERATION, "agent_loop")
                .setAttribute(AGENT_TURN, turn.turnId())
                .setAttribute("agent.session.id", turn.sessionId())
                .startSpan();
        turnSpans.put(turn.turnId(), span);
        turnScopes.put(turn.turnId(), span.makeCurrent());
    }

    @Override
    public void onRoundStarted(AgentTurn turn, int round) {
        Span span = turnSpans.get(turn.turnId());
        if (span != null) {
            span.addEvent("round.start", io.opentelemetry.api.common.Attributes.of(
                    AttributeKey.longKey("agent.round"), (long) round));
        }
    }

    @Override
    public void onToolCallStarted(AgentTurn turn, ModelRound.ToolInvocation invocation) {
        Span span = turnSpans.get(turn.turnId());
        if (span != null) {
            span.addEvent("tool.start", io.opentelemetry.api.common.Attributes.of(
                    AttributeKey.stringKey("gen_ai.tool.name"), invocation.toolName()));
        }
    }

    @Override
    public void onToolCallCompleted(AgentTurn turn, ToolCallFingerprint fingerprint) {
        Span span = turnSpans.get(turn.turnId());
        if (span != null) {
            span.addEvent("tool.end", io.opentelemetry.api.common.Attributes.of(
                    AttributeKey.stringKey("gen_ai.tool.name"), fingerprint.toolName(),
                    AttributeKey.booleanKey("gen_ai.tool.success"), fingerprint.success()));
        }
    }

    @Override
    public void onTurnCompleted(LoopResult result) {
        Span span = turnSpans.remove(result.turn().turnId());
        Scope scope = turnScopes.remove(result.turn().turnId());
        if (scope != null) {
            scope.close();
        }
        if (span != null) {
            span.setAttribute("agent.termination_reason", result.terminationReason().name());
            span.setAttribute("agent.rounds", result.roundsExecuted());
            span.setAttribute("agent.spawn.latency_ms", result.elapsed().toMillis());
            span.setStatus(StatusCode.OK);
            span.end();
        }
    }

    @Override
    public void onTurnFailed(AgentTurn turn, Throwable error) {
        Span span = turnSpans.remove(turn.turnId());
        Scope scope = turnScopes.remove(turn.turnId());
        if (scope != null) {
            scope.close();
        }
        if (span != null) {
            span.recordException(error);
            span.setStatus(StatusCode.ERROR, error.getMessage());
            span.end();
        }
    }
}
