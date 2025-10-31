package com.berailktrk.eShopping.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PaymentMethod token yenileme request DTO'su
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {

    @NotBlank(message = "Kart numarası boş olamaz")
    @Pattern(regexp = "^[0-9\\s]{13,19}$", message = "Geçersiz kart numarası formatı")
    private String cardNumber;

    @NotBlank(message = "CVV kodu boş olamaz")
    @Pattern(regexp = "^[0-9]{3,4}$", message = "CVV kodu 3-4 rakam olmalıdır")
    private String cvv;
}


