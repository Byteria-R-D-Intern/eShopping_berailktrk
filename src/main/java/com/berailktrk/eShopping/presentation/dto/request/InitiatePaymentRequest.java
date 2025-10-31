package com.berailktrk.eShopping.presentation.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ödeme işlemi başlatma request DTO'su
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiatePaymentRequest {

    @NotNull(message = "Sipariş ID boş olamaz")
    private UUID orderId;

    @NotNull(message = "Sıra numarası boş olamaz")
    @Min(value = 1, message = "Sıra numarası 1'den küçük olamaz")
    private Integer sequenceNumber;
}



