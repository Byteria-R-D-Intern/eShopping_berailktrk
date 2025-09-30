package com.berailktrk.eShopping.presentation.dto.request;

import java.math.BigDecimal;
import java.util.Map;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Yeni ürün oluşturma request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {

    @NotBlank(message = "SKU is required")
    @Size(max = 100, message = "SKU must be less than 100 characters")
    private String sku;

    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Product name must be less than 255 characters")
    private String name;

    @Size(max = 2000, message = "Description must be less than 2000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @Size(min = 3, max = 3, message = "Currency must be 3 characters (e.g., TRY, USD)")
    @Builder.Default
    private String currency = "TRY";

    private Map<String, Object> metadata;

    @Builder.Default
    private Boolean isActive = true;

    // Stok bilgisi (opsiyonel - ürün oluştururken stok da eklenebilir)
    @DecimalMin(value = "0", message = "Initial stock quantity cannot be negative")
    private Integer initialStockQuantity;
}
