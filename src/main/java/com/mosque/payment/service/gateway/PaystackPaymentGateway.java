package com.mosque.payment.service.gateway;

import com.mosque.payment.exception.PaymentGatewayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

@Service
public class PaystackPaymentGateway implements PaymentGateway {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String secretKey;
    private final String callbackUrl;

    public PaystackPaymentGateway(RestTemplate restTemplate,
                                  @Value("${paystack.base-url}") String baseUrl,
                                  @Value("${paystack.secret-key}") String secretKey,
                                  @Value("${paystack.callback-url}") String callbackUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.secretKey = secretKey;
        this.callbackUrl = callbackUrl;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String initialize(String email, long amountInKobo, String reference) {
        Map<String, Object> body = Map.of(
                "email", email,
                "amount", amountInKobo,
                "reference", reference,
                "callback_url", callbackUrl);
        try {
            Map<String, Object> response = restTemplate.exchange(
                    baseUrl + "/transaction/initialize", HttpMethod.POST,
                    new HttpEntity<>(body, headers()), Map.class).getBody();

            if (response == null || !Boolean.TRUE.equals(response.get("status"))) {
                throw new PaymentGatewayException("Paystack could not start the transaction");
            }
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            return (String) data.get("authorization_url");
        } catch (RestClientException e) {
            throw new PaymentGatewayException("Could not reach Paystack", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public GatewayVerification verify(String reference) {
        try {
            Map<String, Object> response = restTemplate.exchange(
                    baseUrl + "/transaction/verify/" + reference, HttpMethod.GET,
                    new HttpEntity<>(headers()), Map.class).getBody();

            if (response == null || !Boolean.TRUE.equals(response.get("status"))) {
                return new GatewayVerification(false, 0);
            }
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            boolean paid = "success".equals(data.get("status"));
            long amount = ((Number) data.get("amount")).longValue();
            return new GatewayVerification(paid, amount);
        } catch (RestClientException e) {
            throw new PaymentGatewayException("Could not verify the payment with Paystack", e);
        }
    }

    @Override
    public boolean isValidWebhookSignature(String rawPayload, String signature) {
        if (signature == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            String expected = HexFormat.of().formatHex(mac.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.toLowerCase().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(secretKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
