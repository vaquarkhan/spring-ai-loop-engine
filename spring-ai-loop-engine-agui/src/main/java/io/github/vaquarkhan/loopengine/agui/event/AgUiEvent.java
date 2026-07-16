package io.github.vaquarkhan.loopengine.agui.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

/**
 * AG-UI compatible event payload streamed over SSE.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgUiEvent(
        @JsonProperty("type") String type,
        @JsonProperty("timestamp") Instant timestamp,
        @JsonProperty("runId") String runId,
        @JsonProperty("turnId") String turnId,
        @JsonProperty("payload") Map<String, Object> payload) {

    public static final String RUN_STARTED = "RunStartedEvent";
    public static final String RUN_FINISHED = "RunFinishedEvent";
    public static final String TOOL_CALL_START = "TOOL_CALL_START";
    public static final String TOOL_CALL_END = "TOOL_CALL_END";
    public static final String STATE_DELTA = "STATE_DELTA";
    public static final String APPROVAL_REQUIRED = "APPROVAL_REQUIRED";
    public static final String TEXT_MESSAGE = "TEXT_MESSAGE";
    public static final String ERROR = "ERROR";

    public static AgUiEvent of(String type, String runId, String turnId, Map<String, Object> payload) {
        return new AgUiEvent(type, Instant.now(), runId, turnId, payload);
    }
}
