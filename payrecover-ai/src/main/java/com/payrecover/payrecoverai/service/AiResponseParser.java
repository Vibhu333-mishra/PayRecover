package com.payrecover.payrecoverai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payrecover.payrecoverai.dto.ParsedDiagnosis;
import com.payrecover.payrecoverai.entity.AiSource;
import com.payrecover.payrecoverai.entity.FailureCategory;
import com.payrecover.payrecoverai.entity.RecoveryActionType;
import com.payrecover.payrecoverai.exception.LlmUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * TURNS UNTRUSTED MODEL TEXT INTO A TRUSTED JAVA OBJECT.
 *
 * WHAT "STRUCTURED JSON OUTPUT" MEANS
 * We asked the model to answer with a JSON object instead of a paragraph. JSON
 * is just text with a strict shape, so a program can read specific fields out of
 * it. Without that, we would be doing string-searching on English sentences,
 * which breaks the moment the wording changes.
 *
 * WHY WE STILL DO NOT TRUST IT
 * Asking is not enforcing. Even with response_format=json_object, real models
 * do things like:
 *   - wrap the JSON in ```json ... ``` fences
 *   - add "Here is the analysis:" before it
 *   - return confidence as 91 or "91%" instead of 0.91
 *   - invent a category we never defined
 * Every one of those would crash a naive parser. So this class treats the reply
 * as hostile input and cleans it up. THIS is the difference between a demo that
 * survives a live audience and one that throws a 500 on stage.
 */
@Component
public class AiResponseParser {

    private static final Logger log = LoggerFactory.getLogger(AiResponseParser.class);

    /** Keeps the UI tidy if a model ignores the length limit. */
    private static final int MAX_REASON_LENGTH = 300;
    private static final int MAX_EXPLANATION_LENGTH = 800;

    private final ObjectMapper objectMapper;

    public AiResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedDiagnosis parse(String rawContent, String modelName) {
        String json = extractJsonObject(rawContent);

        JsonNode node;
        try {
            node = objectMapper.readTree(json);
        } catch (Exception ex) {
            throw new LlmUnavailableException(
                    "Model reply was not valid JSON, falling back to rules.", ex);
        }

        // Every read below is defensive: path() never returns null, asText("")
        // supplies a default, and fromCode() maps anything unexpected to a safe
        // value instead of throwing.
        FailureCategory category = FailureCategory.fromCode(
                node.path("failureCategory").asText(""));
        RecoveryActionType action = RecoveryActionType.fromCode(
                node.path("recommendedAction").asText(""));
        double confidence = normaliseConfidence(node.path("confidence").asDouble(0.0));

        String reason = trim(node.path("probableReason").asText(""), MAX_REASON_LENGTH);
        String explanation = trim(node.path("explanation").asText(""), MAX_EXPLANATION_LENGTH);

        if (reason.isBlank()) {
            reason = category.getDisplayName();
        }
        if (explanation.isBlank()) {
            explanation = category.getMerchantHint();
        }

        log.debug("[LLM] Parsed diagnosis: category={} action={} confidence={}",
                category, action, confidence);

        return new ParsedDiagnosis(
                category, reason, action, confidence, explanation, AiSource.LLM, modelName);
    }

    /**
     * Pulls the first {...} block out of whatever the model sent.
     *
     * Handles the three things that actually happen in practice:
     *   1. clean JSON                  -> returned as-is
     *   2. ```json { ... } ```         -> fences stripped
     *   3. "Sure! Here it is: { ... }" -> prose before/after discarded
     *
     * We find the FIRST '{' and the LAST '}' and take everything between them.
     * Crude, but it is exactly the right amount of cleverness for this job.
     */
    private String extractJsonObject(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new LlmUnavailableException("Model reply was empty.");
        }

        String cleaned = raw.trim();

        // Remove markdown code fences if present.
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replace("```json", "").replace("```JSON", "").replace("```", "").trim();
        }

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');

        if (start == -1 || end == -1 || end <= start) {
            throw new LlmUnavailableException(
                    "Model reply contained no JSON object: " + shorten(cleaned));
        }
        return cleaned.substring(start, end + 1);
    }

    /**
     * Forces confidence into the 0.0 - 1.0 range we promised the rest of the app.
     *
     * Models regularly answer 91 (meaning 91%) instead of 0.91. If we stored 91
     * the UI would proudly display "9100% confident". So: anything above 1 is
     * treated as a percentage and divided by 100, then the result is clamped.
     */
    private double normaliseConfidence(double value) {
        double confidence = value;
        if (confidence > 1.0 && confidence <= 100.0) {
            confidence = confidence / 100.0;
        }
        if (confidence < 0.0) {
            confidence = 0.0;
        }
        if (confidence > 1.0) {
            confidence = 1.0;
        }
        // Round to 2 decimals so the UI shows 91% and not 90.99999%.
        return Math.round(confidence * 100.0) / 100.0;
    }

    private String trim(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String value = text.trim();
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String shorten(String text) {
        return text.length() <= 200 ? text : text.substring(0, 200) + "...";
    }
}
