package com.payrecover.payrecoverai.dto;

import com.payrecover.payrecoverai.entity.AiSource;
import com.payrecover.payrecoverai.entity.FailureCategory;
import com.payrecover.payrecoverai.entity.RecoveryActionType;

public class ParsedDiagnosis {

    private final FailureCategory failureCategory;
    private final String probableReason;
    private final RecoveryActionType recommendedAction;
    private final double confidence;      // always 0.0 - 1.0
    private final String explanation;
    private final AiSource source;        // LLM or FALLBACK_RULES
    private final String modelName;

    public ParsedDiagnosis(FailureCategory failureCategory,
                           String probableReason,
                           RecoveryActionType recommendedAction,
                           double confidence,
                           String explanation,
                           AiSource source,
                           String modelName) {
        this.failureCategory = failureCategory;
        this.probableReason = probableReason;
        this.recommendedAction = recommendedAction;
        this.confidence = confidence;
        this.explanation = explanation;
        this.source = source;
        this.modelName = modelName;
    }

    public FailureCategory getFailureCategory() {
        return failureCategory;
    }

    public String getProbableReason() {
        return probableReason;
    }

    public RecoveryActionType getRecommendedAction() {
        return recommendedAction;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getExplanation() {
        return explanation;
    }

    public AiSource getSource() {
        return source;
    }

    public String getModelName() {
        return modelName;
    }
}
