package com.berailktrk.eShopping.presentation.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


//Sepet kalemi response DTO
//Clean Architecture: Sadece data taşıma, business logic yok

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {

    private UUID id;
    private String productSku;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;  // Hesaplanmış toplam fiyat (unitPrice × quantity)
    private Instant addedAt;
}