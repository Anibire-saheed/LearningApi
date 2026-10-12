package com.mosque.payment.data.dto.response;

import com.mosque.payment.data.model.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentSummaryResponse {

    private BigDecimal totalCollected;
    private Map<PaymentType, BigDecimal> totalByType;
}
