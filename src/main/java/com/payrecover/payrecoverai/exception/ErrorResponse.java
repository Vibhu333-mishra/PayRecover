package com.payrecover.payrecoverai.exception;

import java.time.LocalDateTime;

/**
 * Consistent JSON shape for every error the API returns, e.g.:
 * {
 *   "timestamp": "2026-09-02T10:15:30",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Payment not found with ID: PAY9999",
 *   "path": "/api/payments/PAY9999"
 * }
 */
public class ErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    public ErrorResponse() {
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
