package com.berailktrk.eShopping.presentation.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payment response DTO'su
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private UUID id;
    private UUID orderId;
    private UUID userId;
    private UUID paymentMethodId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String transactionId;
    private String authorizationCode;
    private String responseCode;
    private String responseMessage;
    private Instant createdAt;
    private Instant completedAt;
    private Instant failedAt;
    private Instant refundedAt;
    private BigDecimal refundAmount;
}
