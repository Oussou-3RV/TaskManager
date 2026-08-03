package com.orchestration.backend.exception;

public class TaskNotCancellableException extends RuntimeException {
    public TaskNotCancellableException(String message) {
        super(message);
    }
}
