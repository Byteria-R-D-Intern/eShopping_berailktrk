package com.berailktrk.eShopping.presentation.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ödeme iadesi request DTO'su
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundPaymentRequest {

    @NotNull(message = "Payment ID boş olamaz")
    private UUID paymentId;

    @DecimalMin(value = "0.01", message = "İade tutarı 0.01'den büyük olmalıdır")
    private BigDecimal refundAmount; // null ise tam iade
}
