package com.berailktrk.eShopping.application.usecase;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.berailktrk.eShopping.domain.model.AuditLog;
import com.berailktrk.eShopping.domain.model.CartItem;
import com.berailktrk.eShopping.domain.model.Order;
import com.berailktrk.eShopping.domain.model.OrderItem;
import com.berailktrk.eShopping.domain.model.OrderStatus;
import com.berailktrk.eShopping.domain.model.PaymentMethod;
import com.berailktrk.eShopping.domain.model.PaymentStatus;
import com.berailktrk.eShopping.domain.model.Product;
import com.berailktrk.eShopping.domain.model.User;
import com.berailktrk.eShopping.domain.repository.AuditLogRepository;
import com.berailktrk.eShopping.domain.repository.OrderItemRepository;
import com.berailktrk.eShopping.domain.repository.OrderRepository;
import com.berailktrk.eShopping.domain.repository.PaymentMethodRepository;
import com.berailktrk.eShopping.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//Order service - sipariş yönetimi ve business logic
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final AuditLogService auditLogService;
    private final AuditLogRepository auditLogRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final CartService cartService;
    private final OrderItemService orderItemService;

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

    //Sepetten sipariş oluştur - Ana checkout metodu (Sadece online ödeme)
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 30)
    public Order createOrderFromCart(UUID userId, 
                                   Map<String, Object> shippingAddress,
                                   Map<String, Object> billingAddress,
                                   Integer sequenceNumber,
                                   String orderNotes,
                                   Map<String, Object> metadata) {
        
        log.info("Creating order from cart for user: {} with sequence number: {}", userId, sequenceNumber);
        
        // 1. Kullanıcıyı bul
        log.debug("Step 1: Finding user with ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        log.debug("Step 1: User found: {}", user.getEmail());
        
        // 2. Ödeme yöntemini doğrula (sequence number ile)
        log.debug("Step 2: Validating payment method with sequence: {}", sequenceNumber);
        PaymentMethod paymentMethod = validatePaymentMethodBySequence(sequenceNumber, userId);
        log.debug("Step 2: Payment method validated: {}", paymentMethod.getMethodName());
        
        // 3. Sepet içeriğini al
        log.debug("Step 3: Getting cart items for user: {}", userId);
        List<CartItem> cartItems = cartService.getCartItems(userId);
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Sepet boş, sipariş oluşturulamaz");
        }
        log.debug("Step 3: Found {} cart items", cartItems.size());
        
        // 4. Stok kontrolü yap ve rezervasyonları onayla
        log.debug("Step 4: Validating and confirming stock reservations");
        validateAndConfirmStockReservations(cartItems);
        log.debug("Step 4: Stock reservations confirmed");
        
        // 5. Sipariş oluştur ve kaydet
        log.debug("Step 5: Creating order");
        Order order = createOrder(user, cartItems, shippingAddress, billingAddress, paymentMethod, orderNotes, metadata);
        log.debug("Step 5: Order created, saving to database");
        Order savedOrder = orderRepository.save(order);
        log.info("Order oluşturuldu - Order ID: {}", savedOrder.getId());
        
        // 6. OrderItem'ları oluştur ve kaydet
        log.debug("Step 6: Creating order items");
        List<OrderItem> orderItems = orderItemService.createOrderItemsFromCart(cartItems, savedOrder);
        orderItemRepository.saveAll(orderItems);
        log.debug("Step 6: Order items saved");
        
        // 7. CartItems verisini audit log için cache'le (clearCart'ten önce)
        List<Map<String, Object>> cachedCartItemDetails = new ArrayList<>();
        for (CartItem item : cartItems) {
            Map<String, Object> itemDetail = new HashMap<>();
            try {
                Product product = item.getProduct();
                if (product != null) {
                    itemDetail.put("sku", product.getSku());
                    itemDetail.put("name", product.getName());
                }
            } catch (Exception e) {
                log.warn("Could not access product for cart item: {}", item.getId());
            }
            itemDetail.put("quantity", item.getQty());
            itemDetail.put("unitPrice", item.getUnitPriceSnapshot());
            itemDetail.put("totalPrice", item.getUnitPriceSnapshot().multiply(BigDecimal.valueOf(item.getQty())));
            cachedCartItemDetails.add(itemDetail);
        }
        
        // 8. Sepeti temizle (hata durumunda devam et)
        log.debug("Step 8: Clearing cart");
        try {
            cartService.clearCart(userId);
            log.debug("Step 8: Cart cleared successfully");
        } catch (Exception e) {
            log.warn("Failed to clear cart for user: {} - continuing with order creation", userId, e);
            // Sepet temizlenmese bile sipariş oluşturulmuş durumda
        }
        
        // 9. Audit log kaydet
        log.debug("Step 9: Creating audit log");
        logOrderCreation(savedOrder, cachedCartItemDetails, paymentMethod);
        log.debug("Step 9: Audit log created");
        
        log.info("Order created successfully: {} for user: {} with sequence number: {}", 
                savedOrder.getId(), userId, sequenceNumber);
        return savedOrder;
    }

    //Ödeme yöntemini sequence number ile doğrula - Sadece online ödeme yöntemleri
    private PaymentMethod validatePaymentMethodBySequence(Integer sequenceNumber, UUID userId) {
        PaymentMethod paymentMethod = paymentMethodRepository
                .findByUserIdAndSequenceNumberAndIsActiveTrue(userId, sequenceNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                    String.format("Sıra numarası %d ile aktif ödeme yöntemi bulunamadı", sequenceNumber)
                ));
        
        // Sadece online ödeme yöntemlerini kabul et
        String methodType = paymentMethod.getMethodType();
        if (!isOnlinePaymentMethod(methodType)) {
            throw new IllegalArgumentException(
                String.format("Sadece online ödeme yöntemleri kabul edilir. Seçilen yöntem: %s", methodType)
            );
        }
        
        log.info("Payment method validated by sequence: {} for user: {} - Method: {} ({})", 
                sequenceNumber, userId, paymentMethod.getMethodName(), methodType);
        return paymentMethod;
    }

    //Online ödeme yöntemi kontrolü
    private boolean isOnlinePaymentMethod(String methodType) {
        return "CREDIT_CARD".equals(methodType) || 
               "DEBIT_CARD".equals(methodType) || 
               "BANK_TRANSFER".equals(methodType);
    }

    //Stok kontrolü yap ve rezervasyonları onayla
    private void validateAndConfirmStockReservations(List<CartItem> cartItems) {
        for (CartItem cartItem : cartItems) {
            String sku = cartItem.getProduct().getSku();
            Integer quantity = cartItem.getQty();
            
            // Stok kontrolü
            if (!cartService.isStockAvailable(sku, quantity)) {
                throw new IllegalArgumentException("Yetersiz stok: " + sku);
            }
            
            // Rezervasyonu onayla
            boolean confirmed = cartService.confirmStockReservation(sku, quantity);
            if (!confirmed) {
                throw new IllegalStateException("Stok rezervasyonu onaylanamadı: " + sku);
            }
        }
    }

    //Sipariş entity'sini oluştur
    private Order createOrder(User user, 
                            List<CartItem> cartItems,
                            Map<String, Object> shippingAddress,
                            Map<String, Object> billingAddress,
                            PaymentMethod paymentMethod,
                            String orderNotes,
                            Map<String, Object> metadata) {
        
        // Toplam tutarı hesapla
        BigDecimal totalAmount = cartItems.stream()
                .map(item -> item.getUnitPriceSnapshot().multiply(BigDecimal.valueOf(item.getQty())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Billing address yoksa shipping address'i kullan
        if (billingAddress == null || billingAddress.isEmpty()) {
            billingAddress = shippingAddress;
        }
        
        // Metadata'ya sipariş notlarını ve ödeme bilgilerini ekle
        Map<String, Object> finalMetadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        if (orderNotes != null && !orderNotes.trim().isEmpty()) {
            finalMetadata.put("orderNotes", orderNotes);
        }
        finalMetadata.put("createdFromCart", true);
        finalMetadata.put("cartItemCount", cartItems.size());
        finalMetadata.put("paymentMethodId", paymentMethod.getId());
        finalMetadata.put("paymentMethodType", paymentMethod.getMethodType());
        finalMetadata.put("paymentMethodName", paymentMethod.getMethodName());
        finalMetadata.put("isOnlinePayment", true);
        
        return Order.builder()
                .user(user)
                .totalAmount(totalAmount)
                .currency("TRY")
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.NONE)
                .shippingAddress(shippingAddress)
                .billingAddress(billingAddress)
                .metadata(finalMetadata)
                .createdAt(Instant.now())
                .build();
    }

    //Sipariş oluşturma audit log'u
    private void logOrderCreation(Order order, List<Map<String, Object>> cartItemDetails, PaymentMethod paymentMethod) {
        Map<String, Object> details = new HashMap<>();
        details.put("orderId", order.getId());
        details.put("totalAmount", order.getTotalAmount());
        details.put("itemCount", cartItemDetails.size());
        details.put("currency", order.getCurrency());
        details.put("shippingAddress", order.getShippingAddress());
        details.put("billingAddress", order.getBillingAddress());
        details.put("paymentMethodId", paymentMethod.getId());
        details.put("paymentMethodType", paymentMethod.getMethodType());
        details.put("paymentMethodName", paymentMethod.getMethodName());
        details.put("isOnlinePayment", true);
        
        details.put("items", cartItemDetails);
        
        AuditLog orderLog = auditLogService.logOrderActionWithDetails(
            order.getUser(),
            AuditLogService.ACTION_ORDER_CREATED,
            order.getId(),
            String.format("Sipariş oluşturuldu (Online Ödeme): %s, Tutar: %s %s, Ödeme: %s", 
                order.getId(), order.getTotalAmount(), order.getCurrency(), paymentMethod.getMethodName()),
            details
        );
        auditLogRepository.save(orderLog);
    }
}
