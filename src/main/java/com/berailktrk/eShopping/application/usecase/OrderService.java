package com.berailktrk.eShopping.application.usecase;

import java.time.Instant;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.berailktrk.eShopping.domain.model.Order;
import com.berailktrk.eShopping.domain.model.OrderStatus;
import com.berailktrk.eShopping.domain.model.PaymentStatus;

/**
 * Order entity'si için uygulama servis katmanı
 * Sipariş yönetimi ve karmaşık business logic'i yönetir
 */
@Service
public class OrderService {

    /**
     * Siparişin ödeme bekliyor durumunda olup olmadığını kontrol eder
     * 
     * @param order kontrol edilecek sipariş
     * @return ödeme bekliyorsa true
     */
    public boolean isPending(Order order) {
        return order.getStatus() == OrderStatus.PENDING;
    }

    /**
     * Siparişin ödenmiş olup olmadığını kontrol eder
     * 
     * @param order kontrol edilecek sipariş
     * @return ödenmişse true
     */
    public boolean isPaid(Order order) {
        return order.getStatus() == OrderStatus.PAID;
    }

    /**
     * Siparişin iptal edilmiş olup olmadığını kontrol eder
     * 
     * @param order kontrol edilecek sipariş
     * @return iptal edildiyse true
     */
    public boolean isCancelled(Order order) {
        return order.getStatus() == OrderStatus.CANCELLED;
    }

    /**
     * Siparişin kargoya verilmiş olup olmadığını kontrol eder
     * 
     * @param order kontrol edilecek sipariş
     * @return kargoya verildiyse true
     */
    public boolean isShipped(Order order) {
        return order.getStatus() == OrderStatus.SHIPPED;
    }

    /**
     * Siparişin başarısız olup olmadığını kontrol eder
     * 
     * @param order kontrol edilecek sipariş
     * @return başarısızsa true
     */
    public boolean isFailed(Order order) {
        return order.getStatus() == OrderStatus.FAILED;
    }

    /**
     * Siparişin iptal edilebilir durumda olup olmadığını kontrol eder
     * 
     * @param order kontrol edilecek sipariş
     * @return iptal edilebilirse true
     */
    public boolean canBeCancelled(Order order) {
        // Sadece PENDING veya PAID durumundaki siparişler iptal edilebilir
        return order.getStatus() == OrderStatus.PENDING || 
               order.getStatus() == OrderStatus.PAID;
    }

    /**
     * Siparişin kargoya verilebilir durumda olup olmadığını kontrol eder
     * 
     * @param order kontrol edilecek sipariş
     * @return kargoya verilebilirse true
     */
    public boolean canBeShipped(Order order) {
        // Sadece PAID durumundaki siparişler kargoya verilebilir
        return order.getStatus() == OrderStatus.PAID && 
               order.getPaymentStatus() == PaymentStatus.CAPTURED;
    }

    /**
     * Ödemeyi tamamlar ve sipariş durumunu günceller
     * 
     * @param order güncellenecek sipariş
     * @throws IllegalStateException sipariş ödeme yapılabilir durumda değilse
     */
    public void markAsPaid(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Sadece PENDING durumundaki siparişler ödenebilir");
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaymentStatus(PaymentStatus.CAPTURED);
        order.setPaidAt(Instant.now());
    }

    /**
     * Ödeme yetkisini (authorization) kaydeder
     * 
     * @param order güncellenecek sipariş
     * @throws IllegalStateException sipariş ödeme yapılabilir durumda değilse
     */
    public void authorizePayment(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Sadece PENDING durumundaki siparişler için ödeme yetkisi verilebilir");
        }

        order.setPaymentStatus(PaymentStatus.AUTHORIZED);
    }

    /**
     * Yetkili ödemeyi tahsil eder (capture)
     * 
     * @param order güncellenecek sipariş
     * @throws IllegalStateException ödeme yetkisi verilmemişse
     */
    public void capturePayment(Order order) {
        if (order.getPaymentStatus() != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException("Sadece AUTHORIZED durumundaki ödemeler tahsil edilebilir");
        }

        order.setPaymentStatus(PaymentStatus.CAPTURED);
        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(Instant.now());
    }

    /**
     * Siparişi iptal eder
     * 
     * @param order iptal edilecek sipariş
     * @throws IllegalStateException sipariş iptal edilebilir durumda değilse
     */
    public void cancelOrder(Order order) {
        if (!canBeCancelled(order)) {
            throw new IllegalStateException("Sipariş iptal edilebilir durumda değil");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
    }

    /**
     * Siparişi kargoya verildi olarak işaretler
     * 
     * @param order güncellenecek sipariş
     * @throws IllegalStateException sipariş kargoya verilebilir durumda değilse
     */
    public void markAsShipped(Order order) {
        if (!canBeShipped(order)) {
            throw new IllegalStateException(
                "Sipariş kargoya verilebilir durumda değil. Durum: " + order.getStatus() + 
                ", Ödeme Durumu: " + order.getPaymentStatus()
            );
        }

        order.setStatus(OrderStatus.SHIPPED);
    }

    /**
     * Ödeme başarısız olarak işaretler
     * 
     * @param order güncellenecek sipariş
     * @throws IllegalStateException sipariş PENDING durumunda değilse
     */
    public void markAsFailed(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Sadece PENDING durumundaki siparişler başarısız olarak işaretlenebilir");
        }

        order.setStatus(OrderStatus.FAILED);
    }

    /**
     * Ödemeyi iade eder
     * 
     * @param order güncellenecek sipariş
     * @throws IllegalStateException ödeme iade edilebilir durumda değilse
     */
    public void refundPayment(Order order) {
        if (order.getPaymentStatus() != PaymentStatus.CAPTURED) {
            throw new IllegalStateException("Sadece CAPTURED durumundaki ödemeler iade edilebilir");
        }

        if (order.getStatus() != OrderStatus.CANCELLED && order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("Sadece iptal edilmiş veya ödenmiş siparişler iade edilebilir");
        }

        order.setPaymentStatus(PaymentStatus.REFUNDED);
    }

    /**
     * Kargo adresi günceller
     * 
     * @param order güncellenecek sipariş
     * @param shippingAddress yeni kargo adresi
     * @throws IllegalStateException sipariş kargoya verildiyse
     */
    public void updateShippingAddress(Order order, Map<String, Object> shippingAddress) {
        if (order.getStatus() == OrderStatus.SHIPPED) {
            throw new IllegalStateException("Kargoya verilmiş siparişin adresi değiştirilemez");
        }

        order.setShippingAddress(shippingAddress);
    }

    /**
     * Fatura adresi günceller
     * 
     * @param order güncellenecek sipariş
     * @param billingAddress yeni fatura adresi
     */
    public void updateBillingAddress(Order order, Map<String, Object> billingAddress) {
        order.setBillingAddress(billingAddress);
    }

    /**
     * Metadata günceller
     * 
     * @param order güncellenecek sipariş
     * @param metadata yeni metadata
     */
    public void updateMetadata(Order order, Map<String, Object> metadata) {
        order.setMetadata(metadata);
    }

    /**
     * Metadata'ya yeni bir alan ekler veya günceller
     * 
     * @param order güncellenecek sipariş
     * @param key metadata anahtarı
     * @param value metadata değeri
     */
    public void addOrUpdateMetadataField(Order order, String key, Object value) {
        Map<String, Object> metadata = order.getMetadata();
        if (metadata == null) {
            metadata = new java.util.HashMap<>();
            order.setMetadata(metadata);
        }
        metadata.put(key, value);
    }

    /**
     * Kargo takip numarası ekler
     * 
     * @param order güncellenecek sipariş
     * @param trackingNumber kargo takip numarası
     * @throws IllegalStateException sipariş kargoya verilmediyse
     */
    public void addTrackingNumber(Order order, String trackingNumber) {
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new IllegalStateException("Kargo takip numarası sadece kargoya verilmiş siparişlere eklenebilir");
        }

        addOrUpdateMetadataField(order, "tracking_number", trackingNumber);
    }

    /**
     * Sipariş doğrulama
     * 
     * @param order doğrulanacak sipariş
     * @throws IllegalStateException sipariş geçersizse
     */
    public void validateOrder(Order order) {
        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Sipariş tutarı geçerli olmalıdır");
        }

        if (order.getCurrency() == null || order.getCurrency().length() != 3) {
            throw new IllegalStateException("Para birimi 3 karakter olmalıdır");
        }

        if (order.getShippingAddress() == null || order.getShippingAddress().isEmpty()) {
            throw new IllegalStateException("Kargo adresi boş olamaz");
        }

        // Durum ve ödeme durumu tutarlılığı
        validateStatusConsistency(order);
    }

    /**
     * Sipariş durumu ve ödeme durumu tutarlılığını kontrol eder
     * 
     * @param order kontrol edilecek sipariş
     * @throws IllegalStateException tutarlılık yoksa
     */
    public void validateStatusConsistency(Order order) {
        // PAID durumundaki siparişlerin ödeme durumu CAPTURED olmalı
        if (order.getStatus() == OrderStatus.PAID && 
            order.getPaymentStatus() != PaymentStatus.CAPTURED) {
            throw new IllegalStateException(
                "PAID durumundaki siparişin ödeme durumu CAPTURED olmalıdır"
            );
        }

        // SHIPPED durumundaki siparişler önce PAID olmalı
        if (order.getStatus() == OrderStatus.SHIPPED && 
            order.getPaymentStatus() != PaymentStatus.CAPTURED) {
            throw new IllegalStateException(
                "SHIPPED durumundaki siparişin ödeme durumu CAPTURED olmalıdır"
            );
        }

        // PAID siparişin paid_at'ı olmalı
        if (order.getStatus() == OrderStatus.PAID && order.getPaidAt() == null) {
            throw new IllegalStateException("PAID durumundaki siparişin paid_at alanı dolu olmalıdır");
        }

        // CANCELLED siparişin cancelled_at'ı olmalı
        if (order.getStatus() == OrderStatus.CANCELLED && order.getCancelledAt() == null) {
            throw new IllegalStateException("CANCELLED durumundaki siparişin cancelled_at alanı dolu olmalıdır");
        }
    }

    /**
     * Siparişin ne kadar süredir bekletildiğini hesaplar (saat cinsinden)
     * 
     * @param order kontrol edilecek sipariş
     * @return sipariş oluşturulduğundan beri geçen saat
     */
    public long getHoursSinceCreated(Order order) {
        java.time.Duration duration = java.time.Duration.between(
            order.getCreatedAt(), 
            Instant.now()
        );
        return duration.toHours();
    }

    /**
     * Ödeme yapıldıktan sonra geçen süreyi hesaplar (saat cinsinden)
     * 
     * @param order kontrol edilecek sipariş
     * @return ödeme yapıldıktan sonra geçen saat (ödeme yapılmadıysa null)
     */
    public Long getHoursSincePaid(Order order) {
        if (order.getPaidAt() == null) {
            return null;
        }

        java.time.Duration duration = java.time.Duration.between(
            order.getPaidAt(), 
            Instant.now()
        );
        return duration.toHours();
    }
}
