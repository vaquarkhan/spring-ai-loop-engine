package io.github.vaquarkhan.loopengine.agui.hitl;

import io.github.vaquarkhan.loopengine.core.model.AgentTurn;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Human-in-the-Loop suspension store: pauses sensitive tool calls until approval.
 */
public class HitlApprovalStore {

    private final Map<String, PendingApproval> pending = new ConcurrentHashMap<>();
    private final Map<String, AgentTurn> suspendedTurns = new ConcurrentHashMap<>();

    public PendingApproval requestApproval(AgentTurn turn, String toolName, String arguments, long timeoutSeconds) {
        String approvalId = UUID.randomUUID().toString();
        PendingApproval approval = new PendingApproval(
                approvalId,
                turn.turnId(),
                toolName,
                arguments,
                Instant.now(),
                new CompletableFuture<>());
        pending.put(approvalId, approval);
        turn.suspend("awaiting_approval:" + approvalId);
        suspendedTurns.put(turn.turnId(), turn);
        if (timeoutSeconds > 0) {
            approval.future().orTimeout(timeoutSeconds, TimeUnit.SECONDS);
        }
        return approval;
    }

    public boolean decide(String approvalId, boolean approved, String modifiedArguments) {
        PendingApproval approval = pending.get(approvalId);
        if (approval == null) {
            return false;
        }
        ApprovalDecision decision = new ApprovalDecision(approved, modifiedArguments);
        boolean completed = approval.future().complete(decision);
        if (completed && approved) {
            AgentTurn turn = suspendedTurns.remove(approval.turnId());
            if (turn != null) {
                turn.resume();
            }
        }
        pending.remove(approvalId);
        return completed;
    }

    public Optional<PendingApproval> get(String approvalId) {
        return Optional.ofNullable(pending.get(approvalId));
    }

    public Optional<AgentTurn> suspendedTurn(String turnId) {
        return Optional.ofNullable(suspendedTurns.get(turnId));
    }

    public record PendingApproval(
            String approvalId,
            String turnId,
            String toolName,
            String arguments,
            Instant createdAt,
            CompletableFuture<ApprovalDecision> future) {
    }

    public record ApprovalDecision(boolean approved, String modifiedArguments) {
    }
}
