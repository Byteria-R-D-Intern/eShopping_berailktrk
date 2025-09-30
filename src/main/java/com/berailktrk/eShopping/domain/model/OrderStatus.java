package com.berailktrk.eShopping.domain.model;

/**
 * Sipariş durumu enum'u - PostgreSQL order_status type ile eşleşir
 */
public enum OrderStatus {
    /**
     * Sipariş oluşturuldu, ödeme bekleniyor
     */
    PENDING,
    
    /**
     * Ödeme tamamlandı
     */
    PAID,
    
    /**
     * Ödeme başarısız oldu
     */
    FAILED,
    
    /**
     * Sipariş iptal edildi
     */
    CANCELLED,
    
    /**
     * Sipariş kargoya verildi
     */
    SHIPPED
}
