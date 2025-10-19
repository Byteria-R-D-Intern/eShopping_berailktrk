package com.berailktrk.eShopping.presentation.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payment listesi response DTO'su
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentListResponse {

    private List<PaymentResponse> payments;
    private int totalCount;
}
