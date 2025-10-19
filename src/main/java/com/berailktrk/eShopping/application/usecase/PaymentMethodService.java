package com.berailktrk.eShopping.application.usecase;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.berailktrk.eShopping.domain.model.AuditLog;
import com.berailktrk.eShopping.domain.model.CardInfo;
import com.berailktrk.eShopping.domain.model.PaymentMethod;
import com.berailktrk.eShopping.domain.model.User;
import com.berailktrk.eShopping.domain.repository.AuditLogRepository;
import com.berailktrk.eShopping.domain.repository.PaymentMethodRepository;
import com.berailktrk.eShopping.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PaymentMethodService - Ödeme yöntemlerini yöneten servis
 * 
 * Bu servis:
 * - Ödeme yöntemlerini ekler, günceller, siler
 * - TokenizationService ile entegre çalışır
 * - Varsayılan ödeme yöntemi yönetimi yapar
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final TokenizationService tokenizationService;
    private final AuditLogService auditLogService;

    /**
     * Yeni ödeme yöntemi ekler
     * 
     * @param userId Kullanıcı ID
     * @param methodName Ödeme yöntemi adı
     * @param methodType Ödeme yöntemi türü
     * @param cardNumber Kart numarası
     * @param cardholderName Kart sahibinin adı
     * @param expiryDate Son kullanma tarihi
     * @param cvv CVV kodu
     * @param isDefault Varsayılan ödeme yöntemi mi
     * @return Oluşturulan PaymentMethod
     */
    @Transactional
    public PaymentMethod addPaymentMethod(UUID userId, String methodName, String methodType,
                                        String cardNumber, String cardholderName, String expiryDate, String cvv,
                                        boolean isDefault) {
        log.info("Yeni ödeme yöntemi ekleniyor - User: {}, Method: {}", userId, methodName);
        
        // Kullanıcıyı bul
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı: " + userId));
        
        // Aynı isimde ödeme yöntemi var mı kontrol et
        if (paymentMethodRepository.existsByUserIdAndMethodNameAndIsActiveTrue(userId, methodName)) {
            throw new IllegalArgumentException("Bu isimde bir ödeme yöntemi zaten mevcut: " + methodName);
        }
        
        // Kart bilgilerini tokenize et
        CardInfo cardInfo = tokenizationService.tokenizeCard(cardNumber, cardholderName, expiryDate, cvv);
        
        // Eğer varsayılan olarak işaretleniyorsa, diğer varsayılanları kaldır
        if (isDefault) {
            paymentMethodRepository.clearDefaultPaymentMethods(userId);
        }
        
        // PaymentMethod oluştur
        PaymentMethod paymentMethod = PaymentMethod.builder()
                .user(user)
                .methodName(methodName)
                .methodType(methodType)
                .cardInfo(cardInfo)
                .isDefault(isDefault)
                .isActive(true)
                .createdAt(Instant.now())
                .build();
        
        paymentMethod = paymentMethodRepository.save(paymentMethod);
        
        // Audit log
        AuditLog methodLog = auditLogService.logPaymentMethodAction(
                user,
                AuditLogService.ACTION_PAYMENT_METHOD_ADDED,
                paymentMethod.getId(),
                String.format("Ödeme yöntemi eklendi - Name: %s, Type: %s, Masked: %s", 
                        methodName, methodType, cardInfo.getMaskedCardNumber())
        );
        auditLogRepository.save(methodLog);
        
        log.info("Ödeme yöntemi başarıyla eklendi - ID: {}, Name: {}", paymentMethod.getId(), methodName);
        
        return paymentMethod;
    }

    /**
     * Ödeme yöntemini günceller
     * 
     * @param userId Kullanıcı ID
     * @param paymentMethodId Ödeme yöntemi ID
     * @param methodName Yeni ödeme yöntemi adı
     * @param isDefault Varsayılan ödeme yöntemi mi
     * @return Güncellenmiş PaymentMethod
     */
    @Transactional
    public PaymentMethod updatePaymentMethod(UUID userId, UUID paymentMethodId, String methodName, boolean isDefault) {
        log.info("Ödeme yöntemi güncelleniyor - User: {}, Method ID: {}", userId, paymentMethodId);
        
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new IllegalArgumentException("Ödeme yöntemi bulunamadı: " + paymentMethodId));
        
        if (!paymentMethod.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Bu ödeme yöntemi bu kullanıcıya ait değil");
        }
        
        if (!paymentMethod.getIsActive()) {
            throw new IllegalStateException("Ödeme yöntemi aktif değil");
        }
        
        // Aynı isimde başka ödeme yöntemi var mı kontrol et
        if (!methodName.equals(paymentMethod.getMethodName()) && 
            paymentMethodRepository.existsByUserIdAndMethodNameAndIsActiveTrue(userId, methodName)) {
            throw new IllegalArgumentException("Bu isimde bir ödeme yöntemi zaten mevcut: " + methodName);
        }
        
        // Eğer varsayılan olarak işaretleniyorsa, diğer varsayılanları kaldır
        if (isDefault && !paymentMethod.getIsDefault()) {
            paymentMethodRepository.clearDefaultPaymentMethods(userId);
        }
        
        // PaymentMethod'u güncelle
        paymentMethod.setMethodName(methodName);
        paymentMethod.setIsDefault(isDefault);
        paymentMethod.setUpdatedAt(Instant.now());
        
        paymentMethod = paymentMethodRepository.save(paymentMethod);
        
        // Audit log
        AuditLog methodLog = auditLogService.logPaymentMethodAction(
                paymentMethod.getUser(),
                AuditLogService.ACTION_PAYMENT_METHOD_UPDATED,
                paymentMethod.getId(),
                String.format("Ödeme yöntemi güncellendi - Name: %s, Default: %s", methodName, isDefault)
        );
        auditLogRepository.save(methodLog);
        
        log.info("Ödeme yöntemi başarıyla güncellendi - ID: {}", paymentMethodId);
        
        return paymentMethod;
    }

    /**
     * Ödeme yöntemini siler (soft delete)
     * 
     * @param userId Kullanıcı ID
     * @param paymentMethodId Ödeme yöntemi ID
     */
    @Transactional
    public void deletePaymentMethod(UUID userId, UUID paymentMethodId) {
        log.info("Ödeme yöntemi siliniyor - User: {}, Method ID: {}", userId, paymentMethodId);
        
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new IllegalArgumentException("Ödeme yöntemi bulunamadı: " + paymentMethodId));
        
        if (!paymentMethod.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Bu ödeme yöntemi bu kullanıcıya ait değil");
        }
        
        if (!paymentMethod.getIsActive()) {
            throw new IllegalStateException("Ödeme yöntemi zaten silinmiş");
        }
        
        // PaymentMethod'u soft delete yap
        paymentMethod.setIsActive(false);
        paymentMethod.setUpdatedAt(Instant.now());
        
        paymentMethodRepository.save(paymentMethod);
        
        // Token'ı geçersiz kıl
        if (paymentMethod.getCardInfo() != null && paymentMethod.getCardInfo().getToken() != null) {
            tokenizationService.invalidateToken(paymentMethod.getCardInfo().getToken());
        }
        
        // Audit log
        AuditLog methodLog = auditLogService.logPaymentMethodAction(
                paymentMethod.getUser(),
                AuditLogService.ACTION_PAYMENT_METHOD_DELETED,
                paymentMethod.getId(),
                String.format("Ödeme yöntemi silindi - Name: %s", paymentMethod.getMethodName())
        );
        auditLogRepository.save(methodLog);
        
        log.info("Ödeme yöntemi başarıyla silindi - ID: {}", paymentMethodId);
    }

    /**
     * Kullanıcının ödeme yöntemlerini getirir
     * 
     * @param userId Kullanıcı ID
     * @return Ödeme yöntemi listesi
     */
    public List<PaymentMethod> getPaymentMethodsByUserId(UUID userId) {
        return paymentMethodRepository.findByUserIdAndIsActiveTrueOrderByIsDefaultDescCreatedAtDesc(userId);
    }

    /**
     * Kullanıcının varsayılan ödeme yöntemini getirir
     * 
     * @param userId Kullanıcı ID
     * @return Varsayılan PaymentMethod
     */
    public PaymentMethod getDefaultPaymentMethod(UUID userId) {
        return paymentMethodRepository.findByUserIdAndIsDefaultTrueAndIsActiveTrue(userId)
                .orElse(null);
    }

    /**
     * Belirli bir ödeme yöntemini getirir
     * 
     * @param userId Kullanıcı ID
     * @param paymentMethodId Ödeme yöntemi ID
     * @return PaymentMethod
     */
    public PaymentMethod getPaymentMethodById(UUID userId, UUID paymentMethodId) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new IllegalArgumentException("Ödeme yöntemi bulunamadı: " + paymentMethodId));
        
        if (!paymentMethod.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Bu ödeme yöntemi bu kullanıcıya ait değil");
        }
        
        if (!paymentMethod.getIsActive()) {
            throw new IllegalStateException("Ödeme yöntemi aktif değil");
        }
        
        return paymentMethod;
    }

    /**
     * Kullanıcının ödeme yöntemi sayısını getirir
     * 
     * @param userId Kullanıcı ID
     * @return Ödeme yöntemi sayısı
     */
    public long getPaymentMethodCount(UUID userId) {
        return paymentMethodRepository.countByUserIdAndIsActiveTrue(userId);
    }

    /**
     * Kullanıcının varsayılan ödeme yöntemi var mı kontrol eder
     * 
     * @param userId Kullanıcı ID
     * @return Varsayılan ödeme yöntemi var mı
     */
    public boolean hasDefaultPaymentMethod(UUID userId) {
        return paymentMethodRepository.existsByUserIdAndIsDefaultTrueAndIsActiveTrue(userId);
    }
}
