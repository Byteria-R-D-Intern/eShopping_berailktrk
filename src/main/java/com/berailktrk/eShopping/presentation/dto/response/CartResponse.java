package com.berailktrk.eShopping.presentation.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//Sepet response DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    private UUID cartId;
    private UUID userId;
    private List<CartItemResponse> items;
    private Integer totalItemCount;      // Toplam ürün sayısı (miktar toplamı)
    private Integer uniqueItemCount;     // Farklı ürün sayısı
    private java.math.BigDecimal totalAmount;  // Toplam tutar
    private Instant lastUpdate;
    
}
