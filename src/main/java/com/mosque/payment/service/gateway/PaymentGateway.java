package com.mosque.payment.service.gateway;

public interface PaymentGateway {

    /** Starts a transaction and returns the hosted checkout URL the payer should be sent to. */
    String initialize(String email, long amountInKobo, String reference);

    /** Asks the gateway directly whether the transaction was paid. */
    GatewayVerification verify(String reference);

    /** Checks that a webhook really came from the gateway. */
    boolean isValidWebhookSignature(String rawPayload, String signature);
}
