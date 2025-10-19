package com.berailktrk.eShopping.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ödeme yöntemi ekleme request DTO'su
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddPaymentMethodRequest {

    @NotBlank(message = "Ödeme yöntemi adı boş olamaz")
    @Size(max = 100, message = "Ödeme yöntemi adı 100 karakterden fazla olamaz")
    private String methodName;

    @NotBlank(message = "Ödeme yöntemi türü boş olamaz")
    @Pattern(regexp = "^(CREDIT_CARD|DEBIT_CARD|BANK_TRANSFER)$", message = "Geçersiz ödeme yöntemi türü")
    private String methodType;

    @NotBlank(message = "Kart numarası boş olamaz")
    @Pattern(regexp = "^[0-9\\s]{13,19}$", message = "Geçersiz kart numarası formatı")
    private String cardNumber;

    @NotBlank(message = "Kart sahibinin adı boş olamaz")
    @Size(max = 100, message = "Kart sahibinin adı 100 karakterden fazla olamaz")
    private String cardholderName;

    @NotBlank(message = "Son kullanma tarihi boş olamaz")
    @Pattern(regexp = "^(0[1-9]|1[0-2])/([0-9]{2})$", message = "Son kullanma tarihi MM/YY formatında olmalıdır")
    private String expiryDate;

    @NotBlank(message = "CVV kodu boş olamaz")
    @Pattern(regexp = "^[0-9]{3,4}$", message = "CVV kodu 3-4 rakam olmalıdır")
    private String cvv;

    @Builder.Default
    private Boolean isDefault = false;
}
