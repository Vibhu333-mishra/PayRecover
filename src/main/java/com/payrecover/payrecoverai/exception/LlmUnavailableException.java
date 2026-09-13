package com.payrecover.payrecoverai.exception;

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
