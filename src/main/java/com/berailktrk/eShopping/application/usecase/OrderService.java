package com.berailktrk.eShopping.application.usecase;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.berailktrk.eShopping.domain.model.AuditLog;
import com.berailktrk.eShopping.domain.model.Order;
import com.berailktrk.eShopping.domain.model.OrderStatus;
import com.berailktrk.eShopping.domain.model.PaymentStatus;
import com.berailktrk.eShopping.domain.repository.AuditLogRepository;

//Order service - sipariş yönetimi ve business logic
@Service
public class OrderService {

    private final AuditLogService auditLogService;
    private final AuditLogRepository auditLogRepository;

    public OrderService(AuditLogService auditLogService, AuditLogRepository auditLogRepository) {
        this.auditLogService = auditLogService;
        this.auditLogRepository = auditLogRepository;
    }

    //Siparişin ödeme bekliyor durumunda olup olmadığını kontrol et
    public boolean isPending(Order order) {
        return order.getStatus() == OrderStatus.PENDING;
    }

    //Siparişin ödenmiş olup olmadığını kontrol et
    public boolean isPaid(Order order) {
        return order.getStatus() == OrderStatus.PAID;
    }

    //Siparişin iptal edilmiş olup olmadığını kontrol et
    public boolean isCancelled(Order order) {
        return order.getStatus() == OrderStatus.CANCELLED;
    }

    //Siparişin kargoya verilmiş olup olmadığını kontrol et
    public boolean isShipped(Order order) {
        return order.getStatus() == OrderStatus.SHIPPED;
    }

    //Siparişin başarısız olup olmadığını kontrol et
    public boolean isFailed(Order order) {
        return order.getStatus() == OrderStatus.FAILED;
    }

    //Siparişin iptal edilebilir durumda olup olmadığını kontrol et
    public boolean canBeCancelled(Order order) {
        //Sadece PENDING veya PAID durumundaki siparişler iptal edilebilir
        return order.getStatus() == OrderStatus.PENDING || 
               order.getStatus() == OrderStatus.PAID;
    }

    //Siparişin kargoya verilebilir durumda olup olmadığını kontrol et
    public boolean canBeShipped(Order order) {
        //Sadece PAID durumundaki siparişler kargoya verilebilir
        return order.getStatus() == OrderStatus.PAID && 
               order.getPaymentStatus() == PaymentStatus.CAPTURED;
    }

    //Ödemeyi tamamla ve sipariş durumunu güncelle
    public void markAsPaid(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Sadece PENDING durumundaki siparişler ödenebilir");
        }

        //BEFORE durumu
        Map<String, Object> beforeStatus = new HashMap<>();
        beforeStatus.put("status", order.getStatus().toString());
        beforeStatus.put("paymentStatus", order.getPaymentStatus().toString());
        beforeStatus.put("paidAt", order.getPaidAt());

        order.setStatus(OrderStatus.PAID);
        order.setPaymentStatus(PaymentStatus.CAPTURED);
        order.setPaidAt(Instant.now());

        // AFTER durumu
        Map<String, Object> afterStatus = new HashMap<>();
        afterStatus.put("status", order.getStatus().toString());
        afterStatus.put("paymentStatus", order.getPaymentStatus().toString());
        afterStatus.put("paidAt", order.getPaidAt().toString());

        // Detaylı audit log
        Map<String, Object> details = auditLogService.createBeforeAfterDetails(beforeStatus, afterStatus);
        
        AuditLog orderLog = auditLogService.logOrderActionWithDetails(
            order.getUser(),
            AuditLogService.ACTION_ORDER_PAID,
            order.getId(),
            String.format("Sipariş ödendi: %s", order.getId()),
            details
        );
        auditLogRepository.save(orderLog);
    }

    //Ödeme yetkisini (authorization) kaydet
    public void authorizePayment(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Sadece PENDING durumundaki siparişler için ödeme yetkisi verilebilir");
        }

        order.setPaymentStatus(PaymentStatus.AUTHORIZED);
    }

    //Yetkili ödemeyi tahsil et (capture)
    public void capturePayment(Order order) {
        if (order.getPaymentStatus() != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException("Sadece AUTHORIZED durumundaki ödemeler tahsil edilebilir");
        }

        order.setPaymentStatus(PaymentStatus.CAPTURED);
        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(Instant.now());
    }

    //Siparişi iptal et
    public void cancelOrder(Order order) {
        if (!canBeCancelled(order)) {
            throw new IllegalStateException("Sipariş iptal edilebilir durumda değil");
        }

        //BEFORE durumu
        Map<String, Object> beforeStatus = new HashMap<>();
        beforeStatus.put("status", order.getStatus().toString());
        beforeStatus.put("paymentStatus", order.getPaymentStatus().toString());
        beforeStatus.put("cancelledAt", order.getCancelledAt());

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());

        // AFTER durumu
        Map<String, Object> afterStatus = new HashMap<>();
        afterStatus.put("status", order.getStatus().toString());
        afterStatus.put("paymentStatus", order.getPaymentStatus().toString());
        afterStatus.put("cancelledAt", order.getCancelledAt().toString());

        // Detaylı audit log
        Map<String, Object> details = auditLogService.createBeforeAfterDetails(beforeStatus, afterStatus);
        
        AuditLog cancelLog = auditLogService.logOrderActionWithDetails(
            order.getUser(),
            AuditLogService.ACTION_ORDER_CANCELLED,
            order.getId(),
            String.format("Sipariş iptal edildi: %s", order.getId()),
            details
        );
        auditLogRepository.save(cancelLog);
    }

    //Siparişi kargoya verildi olarak işaretle
    public void markAsShipped(Order order) {
        if (!canBeShipped(order)) {
            throw new IllegalStateException(
                "Sipariş kargoya verilebilir durumda değil. Durum: " + order.getStatus() + 
                ", Ödeme Durumu: " + order.getPaymentStatus()
            );
        }

        //BEFORE durumu
        Map<String, Object> beforeStatus = new HashMap<>();
        beforeStatus.put("status", order.getStatus().toString());
        beforeStatus.put("paymentStatus", order.getPaymentStatus().toString());

        order.setStatus(OrderStatus.SHIPPED);

        // AFTER durumu
        Map<String, Object> afterStatus = new HashMap<>();
        afterStatus.put("status", order.getStatus().toString());
        afterStatus.put("paymentStatus", order.getPaymentStatus().toString());

        // Detaylı audit log
        Map<String, Object> details = auditLogService.createBeforeAfterDetails(beforeStatus, afterStatus);
        
        AuditLog shipLog = auditLogService.logOrderActionWithDetails(
            order.getUser(),
            AuditLogService.ACTION_ORDER_SHIPPED,
            order.getId(),
            String.format("Sipariş kargoya verildi: %s", order.getId()),
            details
        );
        auditLogRepository.save(shipLog);
    }

    //Ödeme başarısız olarak işaretle
    public void markAsFailed(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Sadece PENDING durumundaki siparişler başarısız olarak işaretlenebilir");
        }

        order.setStatus(OrderStatus.FAILED);
    }

    //Ödemeyi iade et
    public void refundPayment(Order order) {
        if (order.getPaymentStatus() != PaymentStatus.CAPTURED) {
            throw new IllegalStateException("Sadece CAPTURED durumundaki ödemeler iade edilebilir");
        }

        if (order.getStatus() != OrderStatus.CANCELLED && order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("Sadece iptal edilmiş veya ödenmiş siparişler iade edilebilir");
        }

        order.setPaymentStatus(PaymentStatus.REFUNDED);
    }

    //Kargo adresi güncelle
    public void updateShippingAddress(Order order, Map<String, Object> shippingAddress) {
        if (order.getStatus() == OrderStatus.SHIPPED) {
            throw new IllegalStateException("Kargoya verilmiş siparişin adresi değiştirilemez");
        }

        order.setShippingAddress(shippingAddress);
    }

    //Fatura adresi güncelle
    public void updateBillingAddress(Order order, Map<String, Object> billingAddress) {
        order.setBillingAddress(billingAddress);
    }

    //Metadata güncelle
    public void updateMetadata(Order order, Map<String, Object> metadata) {
        order.setMetadata(metadata);
    }

    //Metadata'ya yeni bir alan ekle veya güncelle
    public void addOrUpdateMetadataField(Order order, String key, Object value) {
        Map<String, Object> metadata = order.getMetadata();
        if (metadata == null) {
            metadata = new java.util.HashMap<>();
            order.setMetadata(metadata);
        }
        metadata.put(key, value);
    }

    //Kargo takip numarası ekle
    public void addTrackingNumber(Order order, String trackingNumber) {
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new IllegalStateException("Kargo takip numarası sadece kargoya verilmiş siparişlere eklenebilir");
        }

        addOrUpdateMetadataField(order, "tracking_number", trackingNumber);
    }

    //Sipariş doğrulama
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

    //Sipariş durumu ve ödeme durumu tutarlılığını kontrol et
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

    //Siparişin ne kadar süredir bekletildiğini hesapla (saat cinsinden)
    public long getHoursSinceCreated(Order order) {
        java.time.Duration duration = java.time.Duration.between(
            order.getCreatedAt(), 
            Instant.now()
        );
        return duration.toHours();
    }

    //Ödeme yapıldıktan sonra geçen süreyi hesapla (saat cinsinden)
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
