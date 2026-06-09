package com.example.jobqueue.exception;

/**
 * Permanent failures that should not be retried (e.g., invalid input, unsupported operation)
 * Implements Single Responsibility: represents exactly one type of failure
 */
public class PermanentJobException extends JobException {
    public PermanentJobException(String message) {
        super(message);
    }

    public PermanentJobException(String message, Throwable cause) {
        super(message, cause);
    }
}
