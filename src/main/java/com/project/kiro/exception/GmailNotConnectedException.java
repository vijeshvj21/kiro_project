package com.project.kiro.exception;

public class GmailNotConnectedException extends RuntimeException {
    public GmailNotConnectedException(String message) {
        super(message);
    }
}
