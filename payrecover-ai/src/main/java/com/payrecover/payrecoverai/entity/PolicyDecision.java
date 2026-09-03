package com.payrecover.payrecoverai.entity;

/**
 * The Policy Engine's verdict on an AI recommendation.
 *
 * This is the whole safety story of the project in one enum: the LLM produces
 * a RecoveryActionType, and this deterministic verdict is what decides whether
 * anything is allowed to happen.
 *
 * ALLOWED   -> rules passed; the simulated recovery may run.
 * BLOCKED   -> rules failed; nothing runs. (e.g. attempts limit reached)
 * ESCALATED -> rules say a human must decide (e.g. high-value or UNKNOWN cause).
 *
 * Fully wired in Phase 6. Created now because AiDiagnosis, RecoveryActionEntity
 * and AuditLog all reference it.
 */
public enum PolicyDecision {

    ALLOWED("Recovery Allowed", "All policy checks passed."),
    BLOCKED("Recovery Blocked", "At least one policy check failed."),
    ESCALATED("Escalated to Human", "Policy requires a human to decide.");

    private final String displayName;
    private final String description;

    PolicyDecision(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
