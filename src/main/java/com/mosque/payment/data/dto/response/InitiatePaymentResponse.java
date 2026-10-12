package com.mosque.payment.data.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InitiatePaymentResponse {

    private String reference;

    /** Redirect the payer here to pay on Paystack's hosted page. */
    private String authorizationUrl;
}
