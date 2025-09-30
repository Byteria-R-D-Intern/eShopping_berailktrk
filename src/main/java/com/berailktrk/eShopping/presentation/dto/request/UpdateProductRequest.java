package com.berailktrk.eShopping.presentation.dto.request;

import java.math.BigDecimal;
import java.util.Map;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ürün güncelleme request DTO
 * Tüm alanlar opsiyonel - sadece gönderilen alanlar güncellenir
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductRequest {

    @Size(max = 255, message = "Product name must be less than 255 characters")
    private String name;

    @Size(max = 2000, message = "Description must be less than 2000 characters")
    private String description;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @Size(min = 3, max = 3, message = "Currency must be 3 characters (e.g., TRY, USD)")
    private String currency;

    private Map<String, Object> metadata;

    private Boolean isActive;
}
