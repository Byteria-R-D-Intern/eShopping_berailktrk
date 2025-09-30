package com.berailktrk.eShopping.domain.model;

/**
 * Ödeme durumu enum'u - PostgreSQL payment_status type ile eşleşir
 */
public enum PaymentStatus {
    /**
     * Ödeme yok
     */
    NONE,
    
    /**
     * Ödeme yetkilendirildi (bloke edildi ama henüz tahsil edilmedi)
     */
    AUTHORIZED,
    
    /**
     * Ödeme tahsil edildi
     */
    CAPTURED,
    
    /**
     * Ödeme iade edildi
     */
    REFUNDED
}
