package com.mosque.payment.service.payment;

import com.mosque.payment.data.dto.request.InitiatePaymentRequest;
import com.mosque.payment.data.dto.response.BaseResponse;
import com.mosque.payment.data.dto.response.PaymentResponse;
import com.mosque.payment.data.model.PaymentStatus;

import java.util.List;

public interface PaymentService {

    BaseResponse initiatePayment(InitiatePaymentRequest request);

    BaseResponse verifyPayment(String reference);

    BaseResponse handleWebhook(String rawPayload, String signature);

    PaymentResponse getPayment(String reference);

    List<PaymentResponse> getAllPayments(PaymentStatus status);

    BaseResponse getSummary();
}
