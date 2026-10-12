package com.mosque.payment.service.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mosque.payment.data.dto.request.InitiatePaymentRequest;
import com.mosque.payment.data.dto.response.BaseResponse;
import com.mosque.payment.data.dto.response.InitiatePaymentResponse;
import com.mosque.payment.data.dto.response.PaymentResponse;
import com.mosque.payment.data.dto.response.PaymentSummaryResponse;
import com.mosque.payment.data.model.Payment;
import com.mosque.payment.data.model.PaymentStatus;
import com.mosque.payment.data.model.PaymentType;
import com.mosque.payment.exception.InvalidWebhookException;
import com.mosque.payment.exception.PaymentGatewayException;
import com.mosque.payment.exception.PaymentNotFoundException;
import com.mosque.payment.repository.PaymentRepository;
import com.mosque.payment.service.gateway.GatewayVerification;
import com.mosque.payment.service.gateway.PaymentGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final ObjectMapper objectMapper;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              PaymentGateway paymentGateway,
                              ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.objectMapper = objectMapper;
    }

    /**
     * Step 1: save a PENDING payment, ask Paystack for a checkout URL and hand it back.
     * Not @Transactional on purpose: if Paystack fails we still want the FAILED row saved.
     */
    @Override
    public BaseResponse initiatePayment(InitiatePaymentRequest request) {
        Payment payment = new Payment();
        payment.setReference("MQ-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        payment.setPaymentType(request.getPaymentType());
        payment.setAmount(request.getAmount());
        payment.setPayerName(request.getPayerName());
        payment.setPayerEmail(request.getPayerEmail());
        payment.setPayerPhone(request.getPayerPhone());
        payment.setNarration(request.getNarration());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        try {
            String url = paymentGateway.initialize(payment.getPayerEmail(), toKobo(payment.getAmount()), payment.getReference());
            return ok("Payment initiated", new InitiatePaymentResponse(payment.getReference(), url));
        } catch (PaymentGatewayException e) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            throw e;
        }
    }

    /** Step 2 (payer returns from Paystack): confirm the result with Paystack. */
    @Override
    @Transactional
    public BaseResponse verifyPayment(String reference) {
        Payment payment = findPayment(reference);
        settle(payment);
        return ok("Payment " + payment.getStatus(), toResponse(payment));
    }

    /** Step 2 alternative (Paystack calls us): check the signature, then confirm with Paystack anyway. */
    @Override
    @Transactional
    public BaseResponse handleWebhook(String rawPayload, String signature) {
        if (!paymentGateway.isValidWebhookSignature(rawPayload, signature)) {
            throw new InvalidWebhookException("Invalid webhook signature");
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(rawPayload);
        } catch (JsonProcessingException e) {
            return ok("Webhook payload could not be read", null);
        }
        if (!"charge.success".equals(root.path("event").asText())) {
            return ok("Event ignored", null);
        }
        String reference = root.path("data").path("reference").asText();
        paymentRepository.findByReference(reference).ifPresent(this::settle);
        return ok("Webhook processed", null);
    }

    @Override
    public PaymentResponse getPayment(String reference) {
        return toResponse(findPayment(reference));
    }

    @Override
    public List<PaymentResponse> getAllPayments(PaymentStatus status) {
        List<Payment> payments = status == null
                ? paymentRepository.findAllByOrderByCreatedAtDesc()
                : paymentRepository.findByStatusOrderByCreatedAtDesc(status);
        return payments.stream().map(this::toResponse).toList();
    }

    @Override
    public BaseResponse getSummary() {
        Map<PaymentType, BigDecimal> totals = new EnumMap<>(PaymentType.class);
        BigDecimal grandTotal = BigDecimal.ZERO;
        for (Object[] row : paymentRepository.sumByTypeForStatus(PaymentStatus.SUCCESSFUL)) {
            BigDecimal sum = (BigDecimal) row[1];
            totals.put((PaymentType) row[0], sum);
            grandTotal = grandTotal.add(sum);
        }
        return ok("Summary of successful payments", new PaymentSummaryResponse(grandTotal, totals));
    }

    // ---- helpers

    /** Moves a PENDING payment to SUCCESSFUL or FAILED using Paystack's own answer. Safe to call twice. */
    private void settle(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return;
        }
        GatewayVerification result = paymentGateway.verify(payment.getReference());

        if (result.successful() && result.amountInKobo() == toKobo(payment.getAmount())) {
            payment.setStatus(PaymentStatus.SUCCESSFUL);
            payment.setPaidAt(LocalDateTime.now());
        } else if (result.successful()) {
            // Paid, but not the amount we expected: never mark it successful, flag it for the admin.
            payment.setStatus(PaymentStatus.FAILED);
            payment.setNarration("AMOUNT MISMATCH - review manually. " + (payment.getNarration() == null ? "" : payment.getNarration()));
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);
    }

    private Payment findPayment(String reference) {
        return paymentRepository.findByReference(reference)
                .orElseThrow(() -> new PaymentNotFoundException(reference));
    }

    private long toKobo(BigDecimal naira) {
        return naira.movePointRight(2).longValueExact();
    }

    private PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(p.getReference(), p.getPaymentType(), p.getStatus(), p.getAmount(),
                p.getPayerName(), p.getPayerEmail(), p.getNarration(), p.getPaidAt(), p.getCreatedAt());
    }

    private BaseResponse ok(String description, Object data) {
        BaseResponse response = new BaseResponse();
        response.setStatusCode(200);
        response.setDescription(description);
        response.setData(data);
        return response;
    }
}
