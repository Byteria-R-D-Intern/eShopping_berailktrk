package com.berailktrk.eShopping.presentation.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Order response DTO'su
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {

    private UUID id;
    private Long orderNumber;
    private UUID userId;
    private BigDecimal totalAmount;
    private String currency;
    private String status;
    private String paymentStatus;
    private Instant createdAt;
    private Instant paidAt;
    private Instant cancelledAt;
    private Map<String, Object> shippingAddress;
    private Map<String, Object> billingAddress;
    private Map<String, Object> metadata;
}
