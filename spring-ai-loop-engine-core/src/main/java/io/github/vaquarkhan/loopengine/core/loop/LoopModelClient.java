package io.github.vaquarkhan.loopengine.core.loop;

import io.github.vaquarkhan.loopengine.core.model.AgentTurn;
import java.util.List;

/**
 * Abstraction over Spring AI {@code ChatClient} / {@code ChatModel} for one model round.
 * Implementations wrap native Spring AI APIs without replacing them.
 */
@FunctionalInterface
public interface LoopModelClient {

    /**
     * Generate the next model round given the current turn context and message prompt.
     *
     * @param turn current agent turn (round count, fingerprints available)
     * @param messages ordered prompt messages (system + history + user + tool results)
     * @param softWrapUp true when soft-max-rounds was reached; client should bias toward a final answer
     */
    ModelRound generate(AgentTurn turn, List<LoopMessage> messages, boolean softWrapUp);

    /**
     * Simple role/content message used by the loop (provider-agnostic).
     */
    record LoopMessage(String role, String content) {
        public static LoopMessage system(String content) {
            return new LoopMessage("system", content);
        }

        public static LoopMessage user(String content) {
            return new LoopMessage("user", content);
        }

        public static LoopMessage assistant(String content) {
            return new LoopMessage("assistant", content);
        }

        public static LoopMessage tool(String content) {
            return new LoopMessage("tool", content);
        }
    }
}
