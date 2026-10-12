package com.mosque.payment.exception;

public class InvalidWebhookException extends RuntimeException {

    public InvalidWebhookException(String message) {
        super(message);
    }
}
