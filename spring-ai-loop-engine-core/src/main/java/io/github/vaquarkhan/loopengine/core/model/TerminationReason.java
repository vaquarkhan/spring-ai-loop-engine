package io.github.vaquarkhan.loopengine.core.model;

/**
 * Why a loop terminated.
 */
public enum TerminationReason {
    MODEL_COMPLETION,
    SOFT_WRAP_UP,
    HARD_MAX_ROUNDS,
    HITL_SUSPENDED,
    APPROVAL_DENIED,
    VALIDATION_FAILED,
    CANCELLED,
    ERROR
}
