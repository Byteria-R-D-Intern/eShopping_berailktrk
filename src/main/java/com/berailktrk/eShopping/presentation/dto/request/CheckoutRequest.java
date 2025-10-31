package com.berailktrk.eShopping.presentation.dto.request;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Checkout Request DTO - Sepetten sipariş oluşturma için gerekli bilgiler
 * Sadece online ödeme yöntemleri desteklenir (CREDIT_CARD/DEBIT_CARD/BANK_TRANSFER)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Sepetten sipariş oluşturma isteği - Sadece online ödeme")
public class CheckoutRequest {

    @NotNull(message = "Kargo adresi boş olamaz")
    @NotEmpty(message = "Kargo adresi boş olamaz")
    @Schema(description = "Kargo adresi bilgileri", required = true, example = "{\"street\":\"123 Main St\",\"city\":\"Istanbul\",\"postalCode\":\"34000\",\"country\":\"Turkey\"}")
    private Map<String, Object> shippingAddress;

    @Schema(description = "Fatura adresi bilgileri (opsiyonel, boşsa kargo adresi kullanılır)", example = "{\"street\":\"123 Main St\",\"city\":\"Istanbul\",\"postalCode\":\"34000\",\"country\":\"Turkey\"}")
    private Map<String, Object> billingAddress;

    @NotNull(message = "Ödeme yöntemi sıra numarası seçilmelidir")
    @Schema(description = "Kullanılacak ödeme yöntemi sıra numarası", required = true, example = "1")
    private Integer sequenceNumber;

    @Schema(description = "Ek sipariş notları", example = "Kapıda teslim almak istiyorum")
    private String orderNotes;

    @Schema(description = "Ek metadata bilgileri", example = "{\"giftWrap\":true}")
    private Map<String, Object> metadata;
}
