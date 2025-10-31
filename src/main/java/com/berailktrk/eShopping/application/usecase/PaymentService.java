package com.berailktrk.eShopping.application.usecase;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.berailktrk.eShopping.domain.model.AuditLog;
import com.berailktrk.eShopping.domain.model.CardInfo;
import com.berailktrk.eShopping.domain.model.Order;
import com.berailktrk.eShopping.domain.model.Payment;
import com.berailktrk.eShopping.domain.model.PaymentMethod;
import com.berailktrk.eShopping.domain.model.PaymentStatus;
import com.berailktrk.eShopping.domain.repository.AuditLogRepository;
import com.berailktrk.eShopping.domain.repository.OrderRepository;
import com.berailktrk.eShopping.domain.repository.PaymentMethodRepository;
import com.berailktrk.eShopping.domain.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PaymentService - Ödeme işlemlerini yöneten ana servis
 * 
 * Bu servis:
 * - Ödeme işlemlerini başlatır ve yönetir
 * - TokenizationService ile entegre çalışır
 * - OrderService ile entegre çalışır
 * - Audit logging yapar
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final OrderRepository orderRepository;
    private final AuditLogRepository auditLogRepository;
    private final TokenizationService tokenizationService;
    private final AuditLogService auditLogService;
    private final OrderService orderService;

    /**
     * Yeni ödeme işlemi başlatır (sequence number ile)
     * 
     * @param orderId Sipariş ID
     * @param userId Kullanıcı ID
     * @param sequenceNumber Ödeme yöntemi sıra numarası
     * @return Oluşturulan Payment
     */
    @Transactional
    public Payment initiatePayment(UUID orderId, UUID userId, Integer sequenceNumber) {
        log.info("Ödeme işlemi başlatılıyor - Order: {}, User: {}, Sequence: {}", orderId, userId, sequenceNumber);
        
        // Siparişi bul ve doğrula
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Sipariş bulunamadı: " + orderId));
        
        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Bu sipariş bu kullanıcıya ait değil");
        }
        
        if (order.getStatus() != com.berailktrk.eShopping.domain.model.OrderStatus.PENDING) {
            throw new IllegalStateException("Sadece PENDING durumundaki siparişler için ödeme yapılabilir");
        }
        
        // Sequence number ile ödeme yöntemini bul ve doğrula
        PaymentMethod paymentMethod = paymentMethodRepository
                .findByUserIdAndSequenceNumberAndIsActiveTrue(userId, sequenceNumber)
                .orElseThrow(() -> new IllegalArgumentException("Sıra numarası " + sequenceNumber + " ile ödeme yöntemi bulunamadı"));
        
        if (!paymentMethod.getIsActive()) {
            throw new IllegalStateException("Ödeme yöntemi aktif değil");
        }
        
        // Aynı sipariş için aktif ödeme var mı kontrol et
        List<PaymentStatus> activeStatuses = List.of(PaymentStatus.NONE, PaymentStatus.AUTHORIZED);
        paymentRepository.findActivePaymentByOrderId(orderId, activeStatuses)
                .ifPresent(existingPayment -> {
                    throw new IllegalStateException("Bu sipariş için zaten aktif bir ödeme işlemi var: " + existingPayment.getId());
                });
        
        // Payment oluştur
        Payment payment = Payment.builder()
                .order(order)
                .user(order.getUser())
                .paymentMethod(paymentMethod)
                .amount(order.getTotalAmount())
                .currency(order.getCurrency())
                .status(PaymentStatus.NONE)
                .createdAt(Instant.now())
                .build();
        
        payment = paymentRepository.save(payment);
        
        // Audit log
        AuditLog paymentLog = auditLogService.logPaymentAction(
                order.getUser(),
                AuditLogService.ACTION_PAYMENT_INITIATED,
                payment.getId(),
                String.format("Ödeme işlemi başlatıldı - Order: %s, Amount: %s %s", 
                        orderId, order.getTotalAmount(), order.getCurrency())
        );
        auditLogRepository.save(paymentLog);
        
        log.info("Ödeme işlemi başlatıldı - Payment ID: {}", payment.getId());
        
        return payment;
    }

    /**
     * Ödeme işlemini authorize eder (yetkilendirir)
     * 
     * @param paymentId Payment ID
     * @return Güncellenmiş Payment
     */
    @Transactional
    public Payment authorizePayment(UUID paymentId) {
        log.info("Ödeme yetkilendirme işlemi başlatılıyor - Payment: {}", paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Ödeme bulunamadı: " + paymentId));
        
        if (payment.getStatus() != PaymentStatus.NONE) {
            throw new IllegalStateException("Sadece NONE durumundaki ödemeler yetkilendirilebilir");
        }
        
        // Token ile kart bilgilerini al
        CardInfo cardInfo = payment.getPaymentMethod().getCardInfo();
        if (cardInfo == null || cardInfo.getToken() == null) {
            throw new IllegalStateException("Ödeme yönteminde geçerli token bulunamadı");
        }
        
        // TokenizationService'den kart bilgilerini doğrula
        CardInfo validatedCardInfo = tokenizationService.getCardInfoByToken(cardInfo.getToken());
        if (validatedCardInfo == null) {
            throw new IllegalStateException("Token geçersiz veya süresi dolmuş");
        }
        
        // BEFORE durumu
        Map<String, Object> beforeStatus = new HashMap<>();
        beforeStatus.put("status", payment.getStatus().toString());
        beforeStatus.put("authorizationCode", payment.getAuthorizationCode());
        beforeStatus.put("transactionId", payment.getTransactionId());
        
        // Mock ödeme sağlayıcısı işlemi (gerçek implementasyonda external API çağrısı yapılır)
        String transactionId = "txn_" + UUID.randomUUID().toString().replace("-", "");
        String authorizationCode = "AUTH_" + System.currentTimeMillis();
        
        // Payment'ı güncelle
        payment.setStatus(PaymentStatus.AUTHORIZED);
        payment.setTransactionId(transactionId);
        payment.setAuthorizationCode(authorizationCode);
        payment.setResponseCode("00");
        payment.setResponseMessage("Authorization successful");
        
        // AFTER durumu
        Map<String, Object> afterStatus = new HashMap<>();
        afterStatus.put("status", payment.getStatus().toString());
        afterStatus.put("authorizationCode", payment.getAuthorizationCode());
        afterStatus.put("transactionId", payment.getTransactionId());
        
        payment = paymentRepository.save(payment);
        
        // Order'ı güncelle
        orderService.authorizePayment(payment.getOrder());
        
        // Detaylı audit log
        Map<String, Object> details = auditLogService.createBeforeAfterDetails(beforeStatus, afterStatus);
        AuditLog authLog = auditLogService.logPaymentActionWithDetails(
                payment.getUser(),
                AuditLogService.ACTION_PAYMENT_AUTHORIZED,
                payment.getId(),
                String.format("Ödeme yetkilendirildi - Amount: %s %s, Transaction: %s", 
                        payment.getAmount(), payment.getCurrency(), transactionId),
                details
        );
        auditLogRepository.save(authLog);
        
        log.info("Ödeme başarıyla yetkilendirildi - Payment: {}, Transaction: {}", paymentId, transactionId);
        
        return payment;
    }

    /**
     * Yetkilendirilmiş ödemeyi capture eder (tahsil eder)
     * 
     * @param paymentId Payment ID
     * @return Güncellenmiş Payment
     */
    @Transactional
    public Payment capturePayment(UUID paymentId) {
        log.info("Ödeme tahsil işlemi başlatılıyor - Payment: {}", paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Ödeme bulunamadı: " + paymentId));
        
        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException("Sadece AUTHORIZED durumundaki ödemeler tahsil edilebilir");
        }
        
        // BEFORE durumu
        Map<String, Object> beforeStatus = new HashMap<>();
        beforeStatus.put("status", payment.getStatus().toString());
        beforeStatus.put("completedAt", payment.getCompletedAt());
        
        // Mock ödeme sağlayıcısı capture işlemi
        String captureCode = "CAP_" + System.currentTimeMillis();
        
        // Payment'ı güncelle
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setCompletedAt(Instant.now());
        payment.setResponseCode("00");
        payment.setResponseMessage("Payment captured successfully");
        
        // Provider response metadata
        Map<String, Object> providerResponse = new HashMap<>();
        providerResponse.put("captureCode", captureCode);
        providerResponse.put("capturedAt", Instant.now().toString());
        payment.setProviderResponse(providerResponse);
        
        // AFTER durumu
        Map<String, Object> afterStatus = new HashMap<>();
        afterStatus.put("status", payment.getStatus().toString());
        afterStatus.put("completedAt", payment.getCompletedAt().toString());
        
        payment = paymentRepository.save(payment);
        
        // Order'ı güncelle
        orderService.capturePayment(payment.getOrder());
        
        // Detaylı audit log
        Map<String, Object> details = auditLogService.createBeforeAfterDetails(beforeStatus, afterStatus);
        AuditLog captureLog = auditLogService.logPaymentActionWithDetails(
                payment.getUser(),
                AuditLogService.ACTION_PAYMENT_CAPTURED,
                payment.getId(),
                String.format("Ödeme tahsil edildi - Amount: %s %s", 
                        payment.getAmount(), payment.getCurrency()),
                details
        );
        auditLogRepository.save(captureLog);
        
        log.info("Ödeme başarıyla tahsil edildi - Payment: {}", paymentId);
        
        return payment;
    }

    /**
     * Ödeme işlemini başarısız olarak işaretler
     * 
     * @param paymentId Payment ID
     * @param errorCode Hata kodu
     * @param errorMessage Hata mesajı
     * @return Güncellenmiş Payment
     */
    @Transactional
    public Payment failPayment(UUID paymentId, String errorCode, String errorMessage) {
        log.info("Ödeme başarısız işaretleniyor - Payment: {}, Error: {}", paymentId, errorMessage);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Ödeme bulunamadı: " + paymentId));
        
        if (payment.getStatus() == PaymentStatus.CAPTURED) {
            throw new IllegalStateException("Tahsil edilmiş ödemeler başarısız olarak işaretlenemez");
        }
        
        // BEFORE durumu
        Map<String, Object> beforeStatus = new HashMap<>();
        beforeStatus.put("status", payment.getStatus().toString());
        beforeStatus.put("failedAt", payment.getFailedAt());
        
        // Payment'ı güncelle
        payment.setFailedAt(Instant.now());
        payment.setResponseCode(errorCode);
        payment.setResponseMessage(errorMessage);
        
        // AFTER durumu
        Map<String, Object> afterStatus = new HashMap<>();
        afterStatus.put("status", payment.getStatus().toString());
        afterStatus.put("failedAt", payment.getFailedAt().toString());
        
        payment = paymentRepository.save(payment);
        
        // Order'ı güncelle
        orderService.markAsFailed(payment.getOrder());
        
        // Detaylı audit log
        Map<String, Object> details = auditLogService.createBeforeAfterDetails(beforeStatus, afterStatus);
        AuditLog failLog = auditLogService.logPaymentActionWithDetails(
                payment.getUser(),
                AuditLogService.ACTION_PAYMENT_FAILED,
                payment.getId(),
                String.format("Ödeme başarısız - Error: %s", errorMessage),
                details
        );
        auditLogRepository.save(failLog);
        
        log.info("Ödeme başarısız olarak işaretlendi - Payment: {}", paymentId);
        
        return payment;
    }

    /**
     * Ödeme iadesi yapar
     * 
     * @param paymentId Payment ID
     * @param refundAmount İade tutarı (null ise tam iade)
     * @return Güncellenmiş Payment
     */
    @Transactional
    public Payment refundPayment(UUID paymentId, BigDecimal refundAmount) {
        log.info("Ödeme iadesi başlatılıyor - Payment: {}, Refund Amount: {}", paymentId, refundAmount);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Ödeme bulunamadı: " + paymentId));
        
        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            throw new IllegalStateException("Sadece CAPTURED durumundaki ödemeler iade edilebilir");
        }
        
        // İade tutarını belirle
        BigDecimal actualRefundAmount = refundAmount != null ? refundAmount : payment.getAmount();
        
        if (actualRefundAmount.compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("İade tutarı ödeme tutarından fazla olamaz");
        }
        
        // BEFORE durumu
        Map<String, Object> beforeStatus = new HashMap<>();
        beforeStatus.put("status", payment.getStatus().toString());
        beforeStatus.put("refundAmount", payment.getRefundAmount());
        beforeStatus.put("refundedAt", payment.getRefundedAt());
        
        // Mock ödeme sağlayıcısı refund işlemi
        String refundCode = "REF_" + System.currentTimeMillis();
        
        // Payment'ı güncelle
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundAmount(actualRefundAmount);
        payment.setRefundedAt(Instant.now());
        payment.setResponseCode("00");
        payment.setResponseMessage("Refund processed successfully");
        
        // Provider response metadata
        Map<String, Object> providerResponse = payment.getProviderResponse();
        if (providerResponse == null) {
            providerResponse = new HashMap<>();
        }
        providerResponse.put("refundCode", refundCode);
        providerResponse.put("refundedAt", Instant.now().toString());
        payment.setProviderResponse(providerResponse);
        
        // AFTER durumu
        Map<String, Object> afterStatus = new HashMap<>();
        afterStatus.put("status", payment.getStatus().toString());
        afterStatus.put("refundAmount", payment.getRefundAmount().toString());
        afterStatus.put("refundedAt", payment.getRefundedAt().toString());
        
        payment = paymentRepository.save(payment);
        
        // Order'ı güncelle
        orderService.refundPayment(payment.getOrder());
        
        // Detaylı audit log
        Map<String, Object> details = auditLogService.createBeforeAfterDetails(beforeStatus, afterStatus);
        AuditLog refundLog = auditLogService.logPaymentActionWithDetails(
                payment.getUser(),
                AuditLogService.ACTION_PAYMENT_REFUNDED,
                payment.getId(),
                String.format("Ödeme iade edildi - Refund Amount: %s %s", 
                        actualRefundAmount, payment.getCurrency()),
                details
        );
        auditLogRepository.save(refundLog);
        
        log.info("Ödeme başarıyla iade edildi - Payment: {}, Refund: {}", paymentId, actualRefundAmount);
        
        return payment;
    }

    /**
     * Belirli bir siparişe ait ödemeleri getirir
     * 
     * @param orderId Sipariş ID
     * @return Ödeme listesi
     */
    public List<Payment> getPaymentsByOrderId(UUID orderId) {
        return paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
    }

    /**
     * Belirli bir kullanıcıya ait ödemeleri getirir
     * 
     * @param userId Kullanıcı ID
     * @return Ödeme listesi
     */
    public List<Payment> getPaymentsByUserId(UUID userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Transaction ID ile ödeme bulur
     * 
     * @param transactionId Transaction ID
     * @return Payment
     */
    public Payment getPaymentByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction bulunamadı: " + transactionId));
    }

    /**
     * Ödeme yönteminin online ödeme olup olmadığını kontrol eder
     * 
     * @param paymentMethodId Ödeme yöntemi ID
     * @return true eğer online ödeme yöntemi ise
     */
    public boolean isOnlinePaymentMethod(UUID paymentMethodId) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new IllegalArgumentException("Ödeme yöntemi bulunamadı: " + paymentMethodId));
        
        return isOnlinePaymentMethod(paymentMethod.getMethodType());
    }

    /**
     * Ödeme yöntemi tipinin online ödeme olup olmadığını kontrol eder
     * 
     * @param methodType Ödeme yöntemi tipi
     * @return true eğer online ödeme yöntemi ise
     */
    public boolean isOnlinePaymentMethod(String methodType) {
        return "CREDIT_CARD".equals(methodType) || 
               "DEBIT_CARD".equals(methodType) || 
               "BANK_TRANSFER".equals(methodType);
    }

    /**
     * Sipariş için ödeme yöntemini doğrular (sadece online ödeme)
     * 
     * @param orderId Sipariş ID
     * @param paymentMethodId Ödeme yöntemi ID
     * @return PaymentMethod
     */
    public PaymentMethod validateOrderPaymentMethod(UUID orderId, UUID paymentMethodId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Sipariş bulunamadı: " + orderId));
        
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new IllegalArgumentException("Ödeme yöntemi bulunamadı: " + paymentMethodId));
        
        // Kullanıcıya ait mi kontrol et
        if (!paymentMethod.getUser().getId().equals(order.getUser().getId())) {
            throw new IllegalArgumentException("Bu ödeme yöntemi bu kullanıcıya ait değil");
        }
        
        // Sadece online ödeme yöntemlerini kabul et
        if (!isOnlinePaymentMethod(paymentMethod.getMethodType())) {
            throw new IllegalArgumentException(
                String.format("Sadece online ödeme yöntemleri kabul edilir. Seçilen yöntem: %s", 
                    paymentMethod.getMethodType())
            );
        }
        
        log.info("Payment method validated for order: {} - Method: {} ({})", 
                orderId, paymentMethod.getMethodName(), paymentMethod.getMethodType());
        
        return paymentMethod;
    }

    /**
     * Desteklenen online ödeme yöntemlerini listeler
     * 
     * @return Desteklenen ödeme yöntemleri
     */
    public List<String> getSupportedOnlinePaymentMethods() {
        return List.of("CREDIT_CARD", "DEBIT_CARD", "BANK_TRANSFER");
    }
}
