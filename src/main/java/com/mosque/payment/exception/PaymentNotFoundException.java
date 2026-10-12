package com.mosque.payment.exception;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String reference) {
        super("No payment found for reference " + reference);
    }
}
