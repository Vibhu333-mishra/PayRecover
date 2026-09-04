package com.payrecover.payrecoverai.entity;

/**
 * The fixed, closed set of buckets a payment failure can fall into.
 *
 * WHY AN ENUM AND NOT A FREE-TEXT STRING?
 * The LLM is a text generator -- left unconstrained it would happily invent
 * "bank_timeout_issue", "Bank Timeout", or "TIMEOUT_FROM_BANK" on different
 * calls. Our Policy Engine has to make a yes/no financial decision, and you
 * cannot write reliable rules against free text. So we force every AI answer
 * through this enum. Anything we do not recognise becomes UNKNOWN, which the
 * policy engine treats conservatively.
 *
 * This is the single most important "controlled AI" idea in the whole project:
 * the LLM may only pick from a list WE defined.
 */
public enum FailureCategory {

    BANK_TIMEOUT(
            "Bank Timeout",
            "The customer's bank did not respond in time. The money almost certainly never left their account."),

    NETWORK_ERROR(
            "Network Error",
            "The request was lost in transit between us, the gateway, or the bank."),

    INSUFFICIENT_FUNDS(
            "Insufficient Funds",
            "The customer's account did not have enough balance. Retrying now will fail the same way."),

    INVALID_PAYMENT_DETAILS(
            "Invalid Payment Details",
            "Wrong card number, expired card, or a bad UPI ID. The customer must correct something."),

    PAYMENT_METHOD_ERROR(
            "Payment Method Error",
            "The chosen method itself is blocked or unsupported for this transaction."),

    TEMPORARY_PROVIDER_FAILURE(
            "Temporary Provider Failure",
            "The bank or gateway is having a short-lived outage on their side."),

    UNKNOWN(
            "Unknown",
            "We could not confidently classify this failure. A human should look at it.");

    private final String displayName;
    private final String merchantHint;

    FailureCategory(String displayName, String merchantHint) {
        this.displayName = displayName;
        this.merchantHint = merchantHint;
    }

    /** Human-friendly label for the UI, e.g. "Bank Timeout". */
    public String getDisplayName() {
        return displayName;
    }

    /** One-line plain-English note we can show next to the badge. */
    public String getMerchantHint() {
        return merchantHint;
    }

    /**
     * SAFE PARSER -- never throws.
     *
     * Enum.valueOf("bank timeout") would throw IllegalArgumentException and
     * blow up the whole /analyze request. But the LLM's output is untrusted
     * input, exactly like a value typed by a stranger into a form. So we clean
     * it up (trim, uppercase, spaces/dashes to underscores) and fall back to
     * UNKNOWN instead of crashing.
     *
     * @param raw whatever text came back from the LLM (may be null)
     * @return a valid category, never null
     */
    public static FailureCategory fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNKNOWN;
        }
        String normalised = raw.trim()
                .toUpperCase()
                .replace(' ', '_')
                .replace('-', '_');
        for (FailureCategory category : values()) {
            if (category.name().equals(normalised)) {
                return category;
            }
        }
        return UNKNOWN;
    }
}
