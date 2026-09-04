package com.payrecover.payrecoverai.entity;

/**
 * Where a diagnosis actually came from.
 *
 * WHY THIS MATTERS (and why it is stored in the database)
 * Rule 11 of this project says: do not fake AI. If the Groq API key is missing,
 * rate-limited, or the network is down, we still return a diagnosis -- but it
 * comes from a hardcoded lookup table, not from a language model. Recording
 * which one ran means:
 *   1. The UI can honestly show "AI unavailable - fallback rules used".
 *   2. Nobody can accuse the demo of pretending a rule table is AI.
 *   3. You can prove the fallback works by simply unsetting the API key.
 *
 * LLM            -> a real language model produced this.
 * FALLBACK_RULES -> our deterministic lookup table produced this.
 */
public enum AiSource {

    LLM("AI Model", true),
    FALLBACK_RULES("Fallback Rules", false);

    private final String displayName;
    private final boolean aiAvailable;

    AiSource(String displayName, boolean aiAvailable) {
        this.displayName = displayName;
        this.aiAvailable = aiAvailable;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Convenience flag so the frontend does not have to string-compare. */
    public boolean isAiAvailable() {
        return aiAvailable;
    }
}
