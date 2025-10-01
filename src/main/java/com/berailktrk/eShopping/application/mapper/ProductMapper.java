package com.berailktrk.eShopping.application.mapper;

import org.springframework.stereotype.Component;

import com.berailktrk.eShopping.domain.model.Product;
import com.berailktrk.eShopping.presentation.dto.response.ProductResponse;

// Product Entity -> DTO Mapper
@Component
public class ProductMapper {

    // Product entity'sinden ProductResponse oluştur
    public ProductResponse toResponse(Product product) {
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

    // Stok bilgisi ile birlikte ProductResponse oluştur
    public ProductResponse toResponseWithStock(Product product, Integer availableStock, Integer reservedStock) {
        ProductResponse response = toResponse(product);
        response.setAvailableStock(availableStock);
        response.setReservedStock(reservedStock);
        return response;
    }
}

