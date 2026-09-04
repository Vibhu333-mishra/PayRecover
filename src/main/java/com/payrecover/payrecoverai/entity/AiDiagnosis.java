package com.payrecover.payrecoverai.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * One row = one AI analysis of one payment.
 *
 * WHAT IT DOES
 * Stores the *output* of the diagnosis step: what category the failure was put
 * in, what action was recommended, how confident the model was, and the plain
 * English explanation shown in the UI.
 *
 * WHY WE STORE IT INSTEAD OF JUST RETURNING IT
 *  1. Auditability. A financial system must be able to answer "why did you
 *     decide that, on that date?" months later. A response that only lives in
 *     the browser cannot answer that.
 *  2. Cost and speed. Re-analysing the same unchanged payment would burn an API
 *     call every time someone reopens the panel.
 *  3. Reproducibility for the demo. If the venue Wi-Fi dies mid-presentation,
 *     already-analysed payments still display their diagnosis.
 *
 * HOW IT CONNECTS
 *  Payment  --(1 : many)-->  AiDiagnosis  --(1 : many)-->  RecoveryActionEntity
 */
@Entity
@Table(name = "ai_diagnoses")
public class AiDiagnosis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * @ManyToOne = many diagnoses can point at one payment (each re-analysis
     * creates a new row, so history is preserved).
     *
     * FetchType.LAZY means Hibernate does NOT load the Payment until we
     * actually call getPayment(). This is the right default: we convert
     * entities to DTOs ourselves, so we only pay for what we use.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_category", nullable = false, length = 40)
    private FailureCategory failureCategory;

    /** Short technical cause, e.g. "Bank server did not respond within 30s". */
    @Column(name = "probable_reason", length = 500)
    private String probableReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommended_action", nullable = false, length = 40)
    private RecoveryActionType recommendedAction;

    /** 0.0 to 1.0. Displayed as a percentage in the UI. */
    @Column(nullable = false)
    private Double confidence;

    /**
     * columnDefinition = "TEXT" because MySQL VARCHAR has a length limit and an
     * LLM explanation can run long. TEXT holds up to 65,535 characters.
     */
    @Column(columnDefinition = "TEXT")
    private String explanation;

    /** LLM or FALLBACK_RULES -- see AiSource for why this is recorded. */
    @Enumerated(EnumType.STRING)
    @Column(name = "ai_source", nullable = false, length = 20)
    private AiSource aiSource;

    /** e.g. "openai/gpt-oss-20b", or "deterministic-rules-v1" for the fallback. */
    @Column(name = "model_name", length = 80)
    private String modelName;

    /** How long the diagnosis took, in milliseconds. Nice to show in the UI. */
    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public AiDiagnosis() {
    }

    /**
     * @PrePersist runs automatically just before Hibernate INSERTs the row, so
     * we can never forget to set the timestamp at a call site.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
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

    public FailureCategory getFailureCategory() {
        return failureCategory;
    }

    public void setFailureCategory(FailureCategory failureCategory) {
        this.failureCategory = failureCategory;
    }

    public String getProbableReason() {
        return probableReason;
    }

    public void setProbableReason(String probableReason) {
        this.probableReason = probableReason;
    }

    public RecoveryActionType getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(RecoveryActionType recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public AiSource getAiSource() {
        return aiSource;
    }

    public void setAiSource(AiSource aiSource) {
        this.aiSource = aiSource;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
