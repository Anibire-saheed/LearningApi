package com.mosque.payment.data.dto.response;

import com.mosque.payment.data.model.PaymentStatus;
import com.mosque.payment.data.model.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {

    private String reference;
    private PaymentType paymentType;
    private PaymentStatus status;
    private BigDecimal amount;
    private String payerName;
    private String payerEmail;
    private String narration;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
