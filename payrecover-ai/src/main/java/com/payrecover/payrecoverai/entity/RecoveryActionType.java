package com.payrecover.payrecoverai.entity;

/**
 * The only recovery actions that exist in this system.
 *
 * The LLM RECOMMENDS one of these. It never performs one. Phase 6's Policy
 * Engine decides whether the recommendation is allowed, and Phase 7's simulator
 * is the only code that actually changes a payment's state.
 *
 * Keeping the list this short is deliberate: five actions are easy to reason
 * about, easy to write rules for, and easy to explain on a demo slide.
 */
public enum RecoveryActionType {

    RETRY(
            "Retry Now",
            "Attempt the exact same payment again immediately."),

    WAIT_AND_RETRY(
            "Wait and Retry",
            "Hold for a short cool-off period, then attempt the same payment again."),

    ALTERNATE_PAYMENT_METHOD(
            "Try Another Method",
            "Ask the customer to pay with a different method or instrument."),

    ESCALATE(
            "Escalate to Human",
            "Route to the merchant's support team for a manual look."),

    STOP(
            "Stop",
            "Do not retry. Retrying cannot succeed without customer action.");

    private final String displayName;
    private final String description;

    RecoveryActionType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * True if this action means "send the same payment to the bank again".
     * Phase 6's Policy Engine uses this to decide which recommendations need
     * the attempt-limit check applied.
     */
    public boolean isRetryAction() {
        return this == RETRY || this == WAIT_AND_RETRY;
    }

    /**
     * SAFE PARSER -- same reasoning as FailureCategory.fromCode().
     * An unrecognised action degrades to ESCALATE (hand it to a human) rather
     * than to RETRY, because guessing "retry" with someone's money would be
     * the unsafe default.
     */
    public static RecoveryActionType fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return ESCALATE;
        }
        String normalised = raw.trim()
                .toUpperCase()
                .replace(' ', '_')
                .replace('-', '_');
        for (RecoveryActionType action : values()) {
            if (action.name().equals(normalised)) {
                return action;
            }
        }
        return ESCALATE;
    }
}
