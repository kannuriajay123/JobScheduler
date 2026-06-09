package com.example.jobqueue.exception;

/**
 * Base exception for job processing errors.
 * Implements Dependency Inversion: caller depends on abstraction, not concrete exceptions
 */
public abstract class JobException extends Exception {
    public JobException(String message) {
        super(message);
    }

    public JobException(String message, Throwable cause) {
        super(message, cause);
    }
}
