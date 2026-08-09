package com.project.kiro.exception;

public class SyncInProgressException extends RuntimeException {
    public SyncInProgressException(String message) {
        super(message);
    }
}
