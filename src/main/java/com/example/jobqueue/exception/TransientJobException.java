package com.example.jobqueue.exception;

/**
 * Transient failures that can be retried (e.g., network timeout, temporary unavailability)
 * Implements Single Responsibility: represents exactly one type of failure
 */
public class TransientJobException extends JobException {
    public TransientJobException(String message) {
        super(message);
    }

    public TransientJobException(String message, Throwable cause) {
        super(message, cause);
    }
}
