package com.berailktrk.eShopping.presentation.dto.response;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Sepet temizleme response DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartClearResponse {

    private UUID userId;
    private String message;
    private Integer removedItemCount;
    private Instant clearedAt;
    private Boolean success;

}
