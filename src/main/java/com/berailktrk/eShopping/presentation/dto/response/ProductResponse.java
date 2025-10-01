package com.berailktrk.eShopping.presentation.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Ürün response DTO - Sadece veri taşıyıcı (mapping logic yok)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private UUID id;
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private String currency;
    private Boolean isActive;
    private Integer version;
    private Instant createdAt;
    private Instant updatedAt;
    private Map<String, Object> metadata;

    // Stok bilgisi (opsiyonel - inventory service'den gelebilir)
    private Integer availableStock;
    private Integer reservedStock;
}
