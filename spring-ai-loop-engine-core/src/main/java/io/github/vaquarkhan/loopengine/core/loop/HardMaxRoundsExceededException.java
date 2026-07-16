package io.github.vaquarkhan.loopengine.core.loop;

import io.github.vaquarkhan.loopengine.core.model.AgentTurn;
import io.github.vaquarkhan.loopengine.core.model.TerminationReason;

/**
 * Thrown when {@link AgentLoopManager} hits the hard-max-rounds budget,
 * preventing infinite recursive API billing.
 */
public class HardMaxRoundsExceededException extends RuntimeException {

    private final AgentTurn turn;
    private final int hardMaxRounds;

    public HardMaxRoundsExceededException(AgentTurn turn, int hardMaxRounds) {
        super("Hard max rounds exceeded (" + hardMaxRounds + ") for turn " + turn.turnId()
                + " after " + turn.round() + " rounds. Forced stop to prevent runaway billing.");
        this.turn = turn;
        this.hardMaxRounds = hardMaxRounds;
        turn.complete(TerminationReason.HARD_MAX_ROUNDS);
    }

    public AgentTurn turn() {
        return turn;
    }

    public int hardMaxRounds() {
        return hardMaxRounds;
    }
}
