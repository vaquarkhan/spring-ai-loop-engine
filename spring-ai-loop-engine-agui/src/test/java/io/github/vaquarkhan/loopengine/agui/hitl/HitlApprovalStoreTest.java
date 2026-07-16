package io.github.vaquarkhan.loopengine.agui.hitl;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.vaquarkhan.loopengine.core.model.AgentTurn;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class HitlApprovalStoreTest {

    @Test
    void requestAndApproveResumesTurn() throws Exception {
        HitlApprovalStore store = new HitlApprovalStore();
        AgentTurn turn = new AgentTurn("s", "delete prod");

        HitlApprovalStore.PendingApproval pending =
                store.requestApproval(turn, "delete", "{\"id\":9}", 5);

        assertThat(turn.isSuspended()).isTrue();
        assertThat(store.get(pending.approvalId())).isPresent();

        boolean accepted = store.decide(pending.approvalId(), true, "{\"id\":9,\"confirmed\":true}");
        assertThat(accepted).isTrue();
        assertThat(turn.isSuspended()).isFalse();
        assertThat(store.get(pending.approvalId())).isEmpty();

        HitlApprovalStore.ApprovalDecision decision =
                pending.future().get(1, TimeUnit.SECONDS);
        assertThat(decision.approved()).isTrue();
        assertThat(decision.modifiedArguments()).contains("confirmed");
    }

    @Test
    void denyDoesNotResumeAndUnknownIdReturnsFalse() {
        HitlApprovalStore store = new HitlApprovalStore();
        AgentTurn turn = new AgentTurn("s", "risky");
        HitlApprovalStore.PendingApproval pending =
                store.requestApproval(turn, "wipe", "{}", 0);

        assertThat(store.decide(pending.approvalId(), false, null)).isTrue();
        assertThat(turn.isSuspended()).isTrue();
        assertThat(store.decide("missing", true, null)).isFalse();
    }
}
