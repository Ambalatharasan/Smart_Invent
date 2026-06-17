package com.stockwise.api.service;

public class VerificationCooldownException extends RuntimeException {
    private final long secondsRemaining;

    public VerificationCooldownException(long secondsRemaining) {
        super("Please wait " + secondsRemaining + " seconds before requesting another verification code.");
        this.secondsRemaining = secondsRemaining;
    }

    public VerificationCooldownException(String message, long secondsRemaining) {
        super(message);
        this.secondsRemaining = secondsRemaining;
    }

    public long getSecondsRemaining() {
        return secondsRemaining;
    }
}
