package com.berailktrk.eShopping.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ödeme yöntemi güncelleme request DTO'su
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePaymentMethodRequest {

    @NotBlank(message = "Ödeme yöntemi adı boş olamaz")
    @Size(max = 100, message = "Ödeme yöntemi adı 100 karakterden fazla olamaz")
    private String methodName;

    @Builder.Default
    private Boolean isDefault = false;
}
