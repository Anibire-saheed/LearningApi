package com.mosque.payment.service.gateway;

/** What the payment gateway told us when we asked about a transaction. */
public record GatewayVerification(boolean successful, long amountInKobo) {
}
