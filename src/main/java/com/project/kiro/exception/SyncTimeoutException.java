package com.project.kiro.exception;

public class SyncTimeoutException extends RuntimeException {
    public SyncTimeoutException(String message) {
        super(message);
    }
}
