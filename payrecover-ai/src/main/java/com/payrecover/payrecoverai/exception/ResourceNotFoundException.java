package com.payrecover.payrecoverai.exception;

/**
 * Thrown when a payment (or any resource) is looked up by ID and doesn't exist.
 * RuntimeException means we don't have to declare "throws" everywhere --
 * Spring's GlobalExceptionHandler below catches it and turns it into a
 * proper 404 JSON response instead of a raw Java stack trace.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
