package com.payrecover.payrecoverai.dto;

import com.payrecover.payrecoverai.entity.AiSource;
import com.payrecover.payrecoverai.entity.FailureCategory;
import com.payrecover.payrecoverai.entity.RecoveryActionType;

/**
 * The clean, validated result of a diagnosis -- INTERNAL, never sent over HTTP.
 *
 * WHY THIS CLASS EXISTS (it is the key design idea of Phase 5)
 * Two completely different things can produce a diagnosis:
 *   1. AiResponseParser   -- after a successful Groq call
 *   2. FallbackClassifier -- when Groq is unreachable
 * Both return THIS type. So AiDiagnosisService does not need to know or care
 * which one ran; it just saves whatever it is handed. That is why the fallback
 * path is only a few lines instead of a duplicated copy of the save logic.
 *
 * All fields are final and set once in the constructor: once a diagnosis has
 * been produced, nothing downstream can quietly change it before it is audited.
 */
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
