package com.berailktrk.eShopping.presentation.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


 //Sepet kalemi güncelleme request DTO 
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCartItemRequest {

    @NotBlank(message = "Product SKU cannot be blank")
    private String productSku;

    @NotNull(message = "Quantity cannot be null")
    @Min(value = 1, message = "Quantity must be positive")
    private Integer quantity;

}
