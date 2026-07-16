package io.github.vaquarkhan.loopengine.agui.web;

import io.github.vaquarkhan.loopengine.agui.event.AgUiEvent;
import io.github.vaquarkhan.loopengine.agui.event.AgUiEventBridge;
import io.github.vaquarkhan.loopengine.agui.hitl.HitlApprovalStore;
import io.github.vaquarkhan.loopengine.core.config.LoopEngineProperties;
import io.github.vaquarkhan.loopengine.core.loop.AgentLoopManager;
import io.github.vaquarkhan.loopengine.core.loop.LoopRequest;
import io.github.vaquarkhan.loopengine.core.model.AgentTurn;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Reactive WebFlux SSE controller that streams AG-UI events for long-running loops.
 */
@RestController
public class AgUiSseController {

    private final AgentLoopManager loopManager;
    private final AgUiEventBridge eventBridge;
    private final HitlApprovalStore approvalStore;
    private final LoopEngineProperties properties;

    public AgUiSseController(
            AgentLoopManager loopManager,
            AgUiEventBridge eventBridge,
            HitlApprovalStore approvalStore,
            LoopEngineProperties properties) {
        this.loopManager = loopManager;
        this.eventBridge = eventBridge;
        this.approvalStore = approvalStore;
        this.properties = properties;
    }

    /**
     * Starts a loop asynchronously and streams AG-UI events (RunStarted, TOOL_CALL_START, STATE_DELTA, …).
     */
    @PostMapping(path = "${spring.ai.loop.agui.sse-path:/api/loop/ag-ui}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AgUiEvent>> run(@RequestBody RunRequest request) {
        String systemPrompt = blankToDefault(request.systemPrompt(), properties.getSystemPrompt());
        AgentTurn turn = new AgentTurn(
                request.sessionId() == null ? "default" : request.sessionId(),
                request.message());

        LoopRequest loopRequest = LoopRequest.builder()
                .sessionId(turn.sessionId())
                .userMessage(turn.userMessage())
                .systemPrompt(systemPrompt)
                .turn(turn)
                .build();

        Flux<ServerSentEvent<AgUiEvent>> events = eventBridge.stream(turn.turnId())
                .map(event -> ServerSentEvent.builder(event).event(event.type()).build());

        Mono.fromCallable(() -> loopManager.run(loopRequest))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(r -> { }, e -> { });

        return events;
    }

    @PostMapping("${spring.ai.loop.agui.approval-path:/api/loop/approvals}/{approvalId}")
    public Mono<Map<String, Object>> approve(
            @PathVariable("approvalId") String approvalId,
            @RequestBody ApprovalBody body) {
        boolean ok = approvalStore.decide(approvalId, body.approved(), body.modifiedArguments());
        return Mono.just(Map.of("approvalId", approvalId, "accepted", ok));
    }

    @GetMapping(path = "${spring.ai.loop.agui.sse-path:/api/loop/ag-ui}/{turnId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AgUiEvent>> subscribe(@PathVariable("turnId") String turnId) {
        return eventBridge.stream(turnId)
                .map(event -> ServerSentEvent.builder(event).event(event.type()).build());
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public record RunRequest(String sessionId, String message, String systemPrompt) {
    }

    public record ApprovalBody(boolean approved, String modifiedArguments) {
    }
}
