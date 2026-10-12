package com.mosque.payment.controller;

import com.mosque.payment.data.dto.request.InitiatePaymentRequest;
import com.mosque.payment.data.dto.response.BaseResponse;
import com.mosque.payment.data.dto.response.PaymentResponse;
import com.mosque.payment.data.model.PaymentStatus;
import com.mosque.payment.service.payment.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // ---- public (payer) endpoints

    @PostMapping
    public ResponseEntity<?> initiatePayment(@Valid @RequestBody InitiatePaymentRequest request) {
        BaseResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{reference}/verify")
    public ResponseEntity<?> verifyPayment(@PathVariable String reference) {
        BaseResponse response = paymentService.verifyPayment(reference);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{reference}")
    public ResponseEntity<?> getPayment(@PathVariable String reference) {
        PaymentResponse response = paymentService.getPayment(reference);
        return ResponseEntity.ok(response);
    }

    /** Paystack calls this. The raw body is needed so the signature can be checked. */
    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestBody String rawPayload,
                                     @RequestHeader(value = "x-paystack-signature", required = false) String signature) {
        BaseResponse response = paymentService.handleWebhook(rawPayload, signature);
        return ResponseEntity.ok(response);
    }

    // ---- admin endpoints (HTTP Basic, see SecurityConfig)

    @GetMapping("/admin/list")
    public ResponseEntity<?> getAllPayments(@RequestParam(required = false) PaymentStatus status) {
        List<PaymentResponse> responses = paymentService.getAllPayments(status);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/admin/summary")
    public ResponseEntity<?> getSummary() {
        BaseResponse response = paymentService.getSummary();
        return ResponseEntity.ok(response);
    }
}
