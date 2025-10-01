package com.berailktrk.eShopping.presentation.dto.response;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {

    private UUID productId;
    private String productName;
    private String productSku;
    private Integer quantity;
    private Integer reserved;
    private Integer available;
    private Instant updatedAt;
    private Integer version;

    /**
     * Mevcut stok = quantity - reserved
     */
    public Integer getAvailable() {
        if (quantity == null || reserved == null) {
            return 0;
        }
        return Math.max(0, quantity - reserved);
    }
}
