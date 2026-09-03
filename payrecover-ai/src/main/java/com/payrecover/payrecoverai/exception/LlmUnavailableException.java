package com.payrecover.payrecoverai.exception;

/**
 * Thrown by LlmClient whenever we could not get a usable answer out of the
 * language model -- for ANY reason:
 *   - no API key configured
 *   - Groq returned 401 / 429 / 500
 *   - the request timed out
 *   - the response body was empty or unparseable
 *
 * WHY ONE EXCEPTION FOR ALL OF THOSE
 * Because the caller's reaction is identical in every case: give up on the LLM
 * and run the deterministic fallback classifier instead. Having five different
 * exception types would mean five identical catch blocks.
 *
 * WHY RuntimeException AND NOT Exception
 * A checked exception would force `throws LlmUnavailableException` onto every
 * method in the call chain. Spring's convention for infrastructure failures is
 * unchecked exceptions, and it keeps the service code readable.
 *
 * IMPORTANT: this exception is caught inside AiDiagnosisService and never
 * reaches the HTTP layer, which is exactly why /analyze still returns 200 OK
 * with a usable diagnosis when the AI is down.
 */
public class LlmUnavailableException extends RuntimeException {

    public LlmUnavailableException(String message) {
        super(message);
    }

    /**
     * The two-argument form keeps the ORIGINAL exception (the "cause") attached,
     * so the stack trace in your console still shows the real network error.
     * Losing the cause is one of the most common debugging mistakes in Java.
     */
    public LlmUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
