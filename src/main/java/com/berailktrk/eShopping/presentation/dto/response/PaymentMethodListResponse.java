package com.berailktrk.eShopping.presentation.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PaymentMethod listesi response DTO'su
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodListResponse {

    private List<PaymentMethodResponse> paymentMethods;
    private int totalCount;
    private boolean hasDefault;
}
