package com.berailktrk.eShopping.presentation.dto.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CardInfo response DTO'su
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardInfoResponse {

    private String token;
    private String cardholderName;
    private String expiryDate;
    private String maskedCardNumber;
    private String cardType;
    private Instant createdAt;
    private Instant expiresAt;
}
