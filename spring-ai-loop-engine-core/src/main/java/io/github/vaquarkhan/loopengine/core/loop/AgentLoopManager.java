package io.github.vaquarkhan.loopengine.core.loop;

import io.github.vaquarkhan.loopengine.core.config.LoopEngineProperties;
import io.github.vaquarkhan.loopengine.core.model.AgentTurn;
import io.github.vaquarkhan.loopengine.core.model.LoopResult;
import io.github.vaquarkhan.loopengine.core.model.TerminationReason;
import io.github.vaquarkhan.loopengine.core.model.ToolCallFingerprint;
import io.github.vaquarkhan.loopengine.core.tool.ArgumentFingerprinter;
import io.github.vaquarkhan.loopengine.core.tool.ToolExecutor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decorates the Spring AI tool-calling phase with stateful, budgeted agent loops.
 *
 * <p>Unlike recursive {@code ToolCallingAdvisor} chains alone, this manager tracks
 * per-turn state ({@link AgentTurn}), fingerprints tool arguments to block duplicate
 * failed retries, injects a soft wrap-up prompt at soft-max-rounds, and throws
 * {@link HardMaxRoundsExceededException} at hard-max-rounds to stop runaway billing.
 *
 * <p>Prompting does not disappear — it is abstracted. Developers define goals and
 * guardrails; the loop performs tactical self-prompting until the goal is met.
 */
public class AgentLoopManager {

    private static final Logger log = LoggerFactory.getLogger(AgentLoopManager.class);

    public static final String SOFT_WRAP_PROMPT =
            "You have reached the soft interaction budget for this turn. "
                    + "Do not call any more tools. Summarize progress and provide a final answer now.";

    public static final String DUPLICATE_BLOCK_TEMPLATE =
            "{\"error\":\"duplicate_failed_action\",\"message\":\"Tool '%s' with identical arguments "
                    + "already failed in this turn. Choose a different approach or different arguments.\","
                    + "\"prior_output\":%s}";

    private final LoopModelClient modelClient;
    private final ToolExecutor toolExecutor;
    private final LoopEngineProperties properties;
    private final Supplier<List<LoopListener>> listeners;

    public AgentLoopManager(
            LoopModelClient modelClient,
            ToolExecutor toolExecutor,
            LoopEngineProperties properties,
            List<LoopListener> listeners) {
        this(modelClient, toolExecutor, properties,
                () -> listeners == null ? List.of() : List.copyOf(listeners));
    }

    public AgentLoopManager(
            LoopModelClient modelClient,
            ToolExecutor toolExecutor,
            LoopEngineProperties properties,
            Supplier<List<LoopListener>> listeners) {
        this.modelClient = Objects.requireNonNull(modelClient, "modelClient");
        this.toolExecutor = Objects.requireNonNull(toolExecutor, "toolExecutor");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.listeners = listeners == null ? List::of : listeners;
        validateBounds(properties);
    }

    private static void validateBounds(LoopEngineProperties properties) {
        if (properties.getSoftMaxRounds() < 1) {
            throw new IllegalArgumentException("softMaxRounds must be >= 1");
        }
        if (properties.getHardMaxRounds() < properties.getSoftMaxRounds()) {
            throw new IllegalArgumentException("hardMaxRounds must be >= softMaxRounds");
        }
    }

    /**
     * Run a full agent loop for one user message.
     */
    public LoopResult run(LoopRequest request) {
        AgentTurn turn = request.newTurn();
        notifyStarted(turn);

        List<LoopModelClient.LoopMessage> messages = new ArrayList<>();
        if (!request.systemPrompt().isBlank()) {
            messages.add(LoopModelClient.LoopMessage.system(request.systemPrompt()));
        }
        for (String prior : request.conversationHistory()) {
            messages.add(LoopModelClient.LoopMessage.user(prior));
        }
        messages.add(LoopModelClient.LoopMessage.user(request.userMessage()));

        int softMax = request.softMaxRoundsOverride() != null
                ? request.softMaxRoundsOverride()
                : properties.getSoftMaxRounds();
        int hardMax = request.hardMaxRoundsOverride() != null
                ? request.hardMaxRoundsOverride()
                : properties.getHardMaxRounds();
        if (hardMax < softMax) {
            hardMax = softMax;
        }

        String lastContent = "";
        try {
            while (true) {
                int round = turn.nextRound();
                notifyRoundStarted(turn, round);

                if (round > hardMax) {
                    throw new HardMaxRoundsExceededException(turn, hardMax);
                }

                boolean softWrap = round >= softMax;
                if (softWrap && !turn.isSoftWrapInjected()) {
                    messages.add(LoopModelClient.LoopMessage.system(SOFT_WRAP_PROMPT));
                    turn.markSoftWrapInjected();
                    notifySoftWrap(turn, round);
                    log.info("Soft-max-rounds ({}) reached for turn {}; wrap-up prompt injected",
                            softMax, turn.turnId());
                }

                ModelRound modelRound = modelClient.generate(turn, List.copyOf(messages), softWrap);
                lastContent = modelRound.content();

                if (!modelRound.hasToolCalls()) {
                    TerminationReason reason = softWrap
                            ? TerminationReason.SOFT_WRAP_UP
                            : TerminationReason.MODEL_COMPLETION;
                    turn.complete(reason);
                    LoopResult result = new LoopResult(lastContent, reason, round, turn.elapsed(), turn);
                    notifyCompleted(result);
                    return result;
                }

                if (softWrap) {
                    // Soft budget: refuse further tools and force wrap-up on next iteration via prompt.
                    messages.add(LoopModelClient.LoopMessage.assistant(
                            lastContent.isBlank() ? "(tool calls suppressed at soft max)" : lastContent));
                    messages.add(LoopModelClient.LoopMessage.system(
                            "Tool calls are no longer allowed. Provide the final answer only."));
                    continue;
                }

                messages.add(LoopModelClient.LoopMessage.assistant(
                        lastContent.isBlank() ? "(calling tools)" : lastContent));

                for (ModelRound.ToolInvocation invocation : modelRound.toolInvocations()) {
                    String fingerprint = ArgumentFingerprinter.fingerprint(
                            invocation.toolName(), invocation.arguments());

                    if (properties.isBlockDuplicateFailedActions()
                            && turn.isDuplicateFailedAction(invocation.toolName(), fingerprint)) {
                        ToolCallFingerprint prior = turn.priorFailedAction(invocation.toolName(), fingerprint)
                                .orElseThrow();
                        String denial = DUPLICATE_BLOCK_TEMPLATE.formatted(
                                invocation.toolName(),
                                quoteJson(prior.outputSummary()));
                        notifyDuplicateBlocked(turn, invocation, prior);
                        ToolCallFingerprint blocked = new ToolCallFingerprint(
                                invocation.toolName(),
                                fingerprint,
                                invocation.arguments(),
                                false,
                                denial,
                                Instant.now(),
                                round);
                        turn.recordToolCall(blocked);
                        messages.add(LoopModelClient.LoopMessage.tool(denial));
                        continue;
                    }

                    notifyToolStarted(turn, invocation);
                    ToolExecutor.ToolResult toolResult = toolExecutor.execute(turn, invocation);
                    ToolCallFingerprint recorded = new ToolCallFingerprint(
                            invocation.toolName(),
                            fingerprint,
                            invocation.arguments(),
                            toolResult.success(),
                            summarize(toolResult.output()),
                            Instant.now(),
                            round);
                    turn.recordToolCall(recorded);
                    notifyToolCompleted(turn, recorded);
                    messages.add(LoopModelClient.LoopMessage.tool(toolResult.output()));
                }
            }
        }
        catch (HardMaxRoundsExceededException hardStop) {
            notifyFailed(turn, hardStop);
            throw hardStop;
        }
        catch (RuntimeException ex) {
            turn.complete(TerminationReason.ERROR);
            notifyFailed(turn, ex);
            throw ex;
        }
    }

    private static String summarize(String output) {
        if (output == null) {
            return "";
        }
        return output.length() <= 512 ? output : output.substring(0, 512) + "…";
    }

    private static String quoteJson(String value) {
        if (value == null) {
            return "\"\"";
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[") || trimmed.startsWith("\"")) {
            return trimmed;
        }
        return "\"" + trimmed.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private void notifyStarted(AgentTurn turn) {
        for (LoopListener listener : listeners.get()) {
            listener.onTurnStarted(turn);
        }
    }

    private void notifyRoundStarted(AgentTurn turn, int round) {
        for (LoopListener listener : listeners.get()) {
            listener.onRoundStarted(turn, round);
        }
    }

    private void notifySoftWrap(AgentTurn turn, int round) {
        for (LoopListener listener : listeners.get()) {
            listener.onSoftWrapInjected(turn, round);
        }
    }

    private void notifyToolStarted(AgentTurn turn, ModelRound.ToolInvocation invocation) {
        for (LoopListener listener : listeners.get()) {
            listener.onToolCallStarted(turn, invocation);
        }
    }

    private void notifyToolCompleted(AgentTurn turn, ToolCallFingerprint fingerprint) {
        for (LoopListener listener : listeners.get()) {
            listener.onToolCallCompleted(turn, fingerprint);
        }
    }

    private void notifyDuplicateBlocked(
            AgentTurn turn, ModelRound.ToolInvocation invocation, ToolCallFingerprint prior) {
        for (LoopListener listener : listeners.get()) {
            listener.onDuplicateToolBlocked(turn, invocation, prior);
        }
    }

    private void notifyCompleted(LoopResult result) {
        for (LoopListener listener : listeners.get()) {
            listener.onTurnCompleted(result);
        }
    }

    private void notifyFailed(AgentTurn turn, Throwable error) {
        for (LoopListener listener : listeners.get()) {
            listener.onTurnFailed(turn, error);
        }
    }
}
