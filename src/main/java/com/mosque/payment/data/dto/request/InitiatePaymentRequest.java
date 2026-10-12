package com.mosque.payment.data.dto.request;

import com.mosque.payment.data.model.PaymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InitiatePaymentRequest {

    @NotNull
    private PaymentType paymentType;

    @NotNull
    @DecimalMin(value = "100.00", message = "Minimum payment is NGN 100")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal amount;

    @NotBlank
    private String payerName;

    @NotBlank
    @Email
    private String payerEmail;

    private String payerPhone;

    /** Optional note, e.g. "For Ramadan feeding programme". */
    private String narration;
}
