package com.payrecover.payrecoverai.service;

import com.payrecover.payrecoverai.dto.PolicyCheckResultDto;
import com.payrecover.payrecoverai.entity.AiDiagnosis;
import com.payrecover.payrecoverai.entity.FailureCategory;
import com.payrecover.payrecoverai.entity.Payment;
import com.payrecover.payrecoverai.entity.PolicyDecision;
import com.payrecover.payrecoverai.entity.RecoveryActionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure Java deterministic rule engine that evaluates a payment failure and its AI
 * recommendation against strict financial safety policies.
 *
 * STRICT RULE PRECEDENCE (Priority Order):
 * 1. Hard Safety Blocks (Priority 1 -> BLOCKED):
 *    - Retry attempt count >= 2 for RETRY / WAIT_AND_RETRY actions.
 *    - Non-retryable failure category for retry actions.
 *    (Safety blocks ALWAYS take top priority; escalation rules NEVER override a block.)
 *
 * 2. Escalation Guardrails (Priority 2 -> ESCALATED):
 *    - High-value transaction (amount > ₹10,000) when retry is considered.
 *    - Low AI confidence (< 0.60).
 *    - AI recommendation is explicitly ESCALATE.
 *
 * 3. Default Pass (Priority 3 -> ALLOWED):
 *    - All safety and escalation checks passed.
 */
@Service
public class PolicyEngine {

    public static final int MAX_ALLOWED_ATTEMPTS = 2;
    public static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("10000.00");
    public static final double MIN_CONFIDENCE_THRESHOLD = 0.60;

    public static class EvaluationResult {
        private final PolicyDecision decision;
        private final RecoveryActionType finalAction;
        private final String policyReason;
        private final List<PolicyCheckResultDto> checks;

        public EvaluationResult(PolicyDecision decision,
                                RecoveryActionType finalAction,
                                String policyReason,
                                List<PolicyCheckResultDto> checks) {
            this.decision = decision;
            this.finalAction = finalAction;
            this.policyReason = policyReason;
            this.checks = checks;
        }

        public PolicyDecision getDecision() {
            return decision;
        }

        public RecoveryActionType getFinalAction() {
            return finalAction;
        }

        public String getPolicyReason() {
            return policyReason;
        }

        public List<PolicyCheckResultDto> getChecks() {
            return checks;
        }
    }

    public EvaluationResult evaluate(Payment payment, AiDiagnosis diagnosis) {
        List<PolicyCheckResultDto> checks = new ArrayList<>();

        RecoveryActionType recAction = diagnosis.getRecommendedAction();
        FailureCategory category = diagnosis.getFailureCategory();
        double confidence = diagnosis.getConfidence() == null ? 0.0 : diagnosis.getConfidence();
        BigDecimal amount = payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount();
        int attempts = payment.getAttempts() == null ? 1 : payment.getAttempts();

        boolean isRetryAction = recAction != null && recAction.isRetryAction();
        boolean isRetryableCategory = isCategoryRetryable(category);

        // --- CHECK 1: Category Eligibility Check ---
        boolean categoryCheckPassed = !isRetryAction || isRetryableCategory;
        String categoryCheckDetail = isRetryAction
                ? (isRetryableCategory
                    ? "Category '" + (category != null ? category.getDisplayName() : "UNKNOWN") + "' is eligible for retry."
                    : "Category '" + (category != null ? category.getDisplayName() : "UNKNOWN") + "' is non-retryable.")
                : "Action '" + (recAction != null ? recAction.getDisplayName() : "N/A") + "' does not retry existing payment method.";
        checks.add(new PolicyCheckResultDto("Category Eligibility Check", categoryCheckPassed, categoryCheckDetail));

        // --- CHECK 2: Attempt Limit Check ---
        boolean attemptCheckPassed = !isRetryAction || (attempts < MAX_ALLOWED_ATTEMPTS);
        String attemptCheckDetail = isRetryAction
                ? (attempts < MAX_ALLOWED_ATTEMPTS
                    ? "Attempts (" + attempts + ") < Max Allowed (" + MAX_ALLOWED_ATTEMPTS + ")."
                    : "Attempt limit reached (" + attempts + " >= " + MAX_ALLOWED_ATTEMPTS + ").")
                : "No retry attempt limit applicable for " + (recAction != null ? recAction.getDisplayName() : "N/A") + ".";
        checks.add(new PolicyCheckResultDto("Attempt Limit Check", attemptCheckPassed, attemptCheckDetail));

        // --- CHECK 3: High Value Transaction Check ---
        boolean amountCheckPassed = !isRetryAction || (amount.compareTo(HIGH_VALUE_THRESHOLD) <= 0);
        String amountCheckDetail = isRetryAction
                ? (amount.compareTo(HIGH_VALUE_THRESHOLD) <= 0
                    ? "Amount (₹" + amount + ") <= High-Value Threshold (₹" + HIGH_VALUE_THRESHOLD + ")."
                    : "Amount (₹" + amount + ") > High-Value Threshold (₹" + HIGH_VALUE_THRESHOLD + "). Requires human review.")
                : "Amount threshold check passed.";
        checks.add(new PolicyCheckResultDto("High Value Guardrail", amountCheckPassed, amountCheckDetail));

        // --- CHECK 4: Confidence Check ---
        boolean confidenceCheckPassed = (confidence >= MIN_CONFIDENCE_THRESHOLD);
        int confidencePct = (int) Math.round(confidence * 100);
        String confidenceDetail = confidenceCheckPassed
                ? "Confidence (" + confidencePct + "%) >= Minimum Threshold (" + (int)(MIN_CONFIDENCE_THRESHOLD * 100) + "%)."
                : "Low AI Confidence (" + confidencePct + "% < " + (int)(MIN_CONFIDENCE_THRESHOLD * 100) + "%). Requires human verification.";
        checks.add(new PolicyCheckResultDto("AI Confidence Guardrail", confidenceCheckPassed, confidenceDetail));

        // =========================================================================
        // PRIORITY 1: HARD SAFETY BLOCKS (Highest Priority -> BLOCKED)
        // =========================================================================
        if (isRetryAction && !categoryCheckPassed) {
            RecoveryActionType overrideAction = (category == FailureCategory.INSUFFICIENT_FUNDS
                    || category == FailureCategory.PAYMENT_METHOD_ERROR)
                    ? RecoveryActionType.ALTERNATE_PAYMENT_METHOD
                    : RecoveryActionType.STOP;
            String reason = "Recovery BLOCKED: Category '" + (category != null ? category.getDisplayName() : "UNKNOWN")
                    + "' cannot be retried automatically. AI recommended " + recAction.name()
                    + " but policy overridden to " + overrideAction.name() + ".";
            return new EvaluationResult(PolicyDecision.BLOCKED, overrideAction, reason, checks);
        }

        if (isRetryAction && !attemptCheckPassed) {
            String reason = "Recovery BLOCKED: Maximum retry attempts reached ("
                    + attempts + " >= " + MAX_ALLOWED_ATTEMPTS + "). AI recommended "
                    + recAction.name() + " but policy overridden to STOP.";
            return new EvaluationResult(PolicyDecision.BLOCKED, RecoveryActionType.STOP, reason, checks);
        }

        if (recAction == RecoveryActionType.STOP) {
            String reason = "Recovery BLOCKED: AI recommended STOP. Further attempts blocked by policy.";
            return new EvaluationResult(PolicyDecision.BLOCKED, RecoveryActionType.STOP, reason, checks);
        }

        // =========================================================================
        // PRIORITY 2: ESCALATION GUARDRAILS (Second Priority -> ESCALATED)
        // =========================================================================
        if (isRetryAction && !amountCheckPassed) {
            String reason = "Recovery ESCALATED: High transaction amount (₹" + amount
                    + " > ₹" + HIGH_VALUE_THRESHOLD + "). Requires manual merchant authorization.";
            return new EvaluationResult(PolicyDecision.ESCALATED, RecoveryActionType.ESCALATE, reason, checks);
        }

        if (!confidenceCheckPassed) {
            String reason = "Recovery ESCALATED: AI confidence level is too low ("
                    + confidencePct + "% < 60%). Handed to human reviewer for safety.";
            return new EvaluationResult(PolicyDecision.ESCALATED, RecoveryActionType.ESCALATE, reason, checks);
        }

        if (recAction == RecoveryActionType.ESCALATE) {
            String reason = "Recovery ESCALATED: AI recommended human escalation.";
            return new EvaluationResult(PolicyDecision.ESCALATED, RecoveryActionType.ESCALATE, reason, checks);
        }

        // =========================================================================
        // PRIORITY 3: DEFAULT PASS (Priority 3 -> ALLOWED)
        // =========================================================================
        String reason = "Recovery ALLOWED: All policy and safety checks passed successfully.";
        return new EvaluationResult(PolicyDecision.ALLOWED, recAction, reason, checks);
    }

    private boolean isCategoryRetryable(FailureCategory category) {
        if (category == null) return false;
        return category == FailureCategory.BANK_TIMEOUT
                || category == FailureCategory.NETWORK_ERROR
                || category == FailureCategory.TEMPORARY_PROVIDER_FAILURE;
    }
}
