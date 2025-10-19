package com.berailktrk.eShopping.presentation.dto.response;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PaymentMethod response DTO'su
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodResponse {

    private UUID id;
    private String methodName;
    private String methodType;
    private CardInfoResponse cardInfo;
    private Boolean isDefault;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
