package com.berailktrk.eShopping.domain.model;

/**
 * Bildirim türlerini temsil eden enum
 */
public enum NotificationType {
    // E-ticaret bildirimleri
    ORDER_CREATED,
    ORDER_PAID,
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    ORDER_CANCELLED,
    PAYMENT_FAILED,
    PRODUCT_IN_STOCK,
    PRODUCT_OUT_OF_STOCK,
    PRICE_DROP,
    CART_REMINDER,
    ACCOUNT_ACTIVATION,
    SECURITY_ALERT,
    PROMOTION,
    
    // Sistem bildirimleri
    SYSTEM
}
