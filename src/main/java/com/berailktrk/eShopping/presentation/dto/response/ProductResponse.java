package com.berailktrk.eShopping.presentation.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.berailktrk.eShopping.domain.model.Product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ürün response DTO
 */
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

    /**
     * Product entity'sinden ProductResponse oluşturur
     */
    public static ProductResponse fromProduct(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .currency(product.getCurrency())
                .isActive(product.getIsActive())
                .version(product.getVersion())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .metadata(product.getMetadata())
                .build();
    }

    /**
     * Stok bilgisi ile birlikte ProductResponse oluşturur
     */
    public static ProductResponse fromProductWithStock(Product product, Integer availableStock, Integer reservedStock) {
        ProductResponse response = fromProduct(product);
        response.setAvailableStock(availableStock);
        response.setReservedStock(reservedStock);
        return response;
    }
}
