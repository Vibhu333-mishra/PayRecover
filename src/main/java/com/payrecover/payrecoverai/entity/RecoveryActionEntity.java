package com.payrecover.payrecoverai.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One row = one recovery attempt on one payment.
 *
 * WHAT IT DOES
 * This is the record of the *decision chain* for a single recovery:
 *   what the AI recommended  ->  what the Policy Engine decided  ->
 *   what we actually did     ->  what happened.
 *
 * WHY THE NAME HAS "Entity" ON THE END
 * Because RecoveryActionType is already taken by the enum. Calling this class
 * RecoveryAction too would mean two very similar names in the same package and
 * constant import confusion. The "...Entity" suffix makes it unmistakable which
 * one is the database table.
 *
 * WHEN IT GETS FILLED IN
 * Phase 5 (this phase) only creates the table. Phase 6 writes the policy
 * verdict into it and Phase 7's simulator writes the outcome. It is created now
 * because the audit trail and the metrics both need to read from it.
 */
@Entity
@Table(name = "recovery_actions")
public class RecoveryActionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    /**
     * Which diagnosis triggered this attempt. Nullable because a human could in
     * principle trigger a recovery without an AI diagnosis existing first.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnosis_id")
    private AiDiagnosis diagnosis;

    /** What the AI asked for. */
    @Enumerated(EnumType.STRING)
    @Column(name = "recommended_action", nullable = false, length = 40)
    private RecoveryActionType recommendedAction;

    /** What the deterministic Policy Engine said about that request. */
    @Enumerated(EnumType.STRING)
    @Column(name = "policy_decision", nullable = false, length = 20)
    private PolicyDecision policyDecision;

    /** Human-readable list of which checks passed or failed. */
    @Column(name = "policy_reason", columnDefinition = "TEXT")
    private String policyReason;

    /**
     * What was ACTUALLY executed. This can differ from recommendedAction --
     * that difference is the proof that the policy engine really does override
     * the AI. (AI said RETRY, attempts were maxed out, finalAction = ESCALATE.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "final_action", nullable = false, length = 40)
    private RecoveryActionType finalAction;

    /** Which attempt this was for the payment (2 = first retry, and so on). */
    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    /**
     * Result of the simulated attempt. One of:
     *   "RECOVERED"     - the simulated retry succeeded
     *   "FAILED_AGAIN"  - the simulated retry failed
     *   "NOT_ATTEMPTED" - policy blocked it, so nothing was sent
     *   "ESCALATED"     - handed to a human instead
     */
    @Column(nullable = false, length = 20)
    private String outcome;

    /** The amount won back. Zero unless outcome is RECOVERED. */
    @Column(name = "amount_recovered", precision = 10, scale = 2)
    private BigDecimal amountRecovered;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public RecoveryActionEntity() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (amountRecovered == null) {
            amountRecovered = BigDecimal.ZERO;
        }
    }

    // ===== Getters and Setters =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public AiDiagnosis getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(AiDiagnosis diagnosis) {
        this.diagnosis = diagnosis;
    }

    public RecoveryActionType getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(RecoveryActionType recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public PolicyDecision getPolicyDecision() {
        return policyDecision;
    }

    public void setPolicyDecision(PolicyDecision policyDecision) {
        this.policyDecision = policyDecision;
    }

    public String getPolicyReason() {
        return policyReason;
    }

    public void setPolicyReason(String policyReason) {
        this.policyReason = policyReason;
    }

    public RecoveryActionType getFinalAction() {
        return finalAction;
    }

    public void setFinalAction(RecoveryActionType finalAction) {
        this.finalAction = finalAction;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public BigDecimal getAmountRecovered() {
        return amountRecovered;
    }

    public void setAmountRecovered(BigDecimal amountRecovered) {
        this.amountRecovered = amountRecovered;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
