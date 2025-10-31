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
        
        // Yeni sequence number belirle (en yüksek + 1)
        Integer nextSequenceNumber = paymentMethodRepository.findMaxSequenceNumberByUserId(userId) + 1;
        
        // PaymentMethod oluştur
        PaymentMethod paymentMethod = PaymentMethod.builder()
                .user(user)
                .userName(user.getEmail())
                .methodName(methodName)
                .methodType(methodType)
                .sequenceNumber(nextSequenceNumber)
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
        
        // Sequence number'ları yeniden düzenle
        reorderSequenceNumbers(userId);
        
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

    // ==================== SEQUENCE NUMBER İŞLEMLERİ ====================

    /**
     * Kullanıcının ödeme yöntemlerini sequence number'a göre sıralı getirir
     * 
     * @param userId Kullanıcı ID
     * @return Sıralı ödeme yöntemleri listesi
     */
    public List<PaymentMethod> getPaymentMethodsBySequence(UUID userId) {
        log.info("Sequence number'a göre ödeme yöntemleri getiriliyor - User: {}", userId);
        
        List<PaymentMethod> paymentMethods = paymentMethodRepository
                .findByUserIdAndIsActiveTrueOrderBySequenceNumberAsc(userId);
        
        log.info("{} adet ödeme yöntemi bulundu - User: {}", paymentMethods.size(), userId);
        return paymentMethods;
    }

    /**
     * Belirli sequence number'a sahip ödeme yöntemini getirir
     * 
     * @param userId Kullanıcı ID
     * @param sequenceNumber Sıra numarası
     * @return PaymentMethod
     * @throws IllegalArgumentException Sequence number bulunamazsa
     */
    public PaymentMethod getPaymentMethodBySequence(UUID userId, Integer sequenceNumber) {
        log.info("Sequence number ile ödeme yöntemi getiriliyor - User: {}, Sequence: {}", userId, sequenceNumber);
        
        PaymentMethod paymentMethod = paymentMethodRepository
                .findByUserIdAndSequenceNumberAndIsActiveTrue(userId, sequenceNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Sequence number %d'ye sahip ödeme yöntemi bulunamadı", sequenceNumber)));
        
        log.info("Ödeme yöntemi bulundu - ID: {}, Name: {}", paymentMethod.getId(), paymentMethod.getMethodName());
        return paymentMethod;
    }

    /**
     * Ödeme yönteminin sequence number'ını günceller (sequence number ile)
     * 
     * @param userId Kullanıcı ID
     * @param currentSequenceNumber Mevcut sıra numarası
     * @param newSequenceNumber Yeni sıra numarası
     * @return Güncellenmiş PaymentMethod
     */
    @Transactional
    public PaymentMethod updateSequenceNumber(UUID userId, Integer currentSequenceNumber, Integer newSequenceNumber) {
        log.info("Sequence number güncelleniyor - User: {}, Current Sequence: {}, New Sequence: {}", 
                userId, currentSequenceNumber, newSequenceNumber);
        
        // Mevcut sequence number ile ödeme yöntemini bul
        PaymentMethod paymentMethod = getPaymentMethodBySequence(userId, currentSequenceNumber);
        
        // Yeni sequence number zaten kullanılıyor mu kontrol et
        if (paymentMethodRepository.existsByUserIdAndSequenceNumberAndIsActiveTrue(userId, newSequenceNumber)) {
            throw new IllegalArgumentException("Bu sıra numarası zaten kullanılıyor: " + newSequenceNumber);
        }
        
        // Sequence number'ı güncelle
        paymentMethod.setSequenceNumber(newSequenceNumber);
        paymentMethod.setUpdatedAt(Instant.now());
        
        paymentMethod = paymentMethodRepository.save(paymentMethod);
        
        // Audit log
        User user = paymentMethod.getUser();
        AuditLog auditLog = auditLogService.logPaymentMethodAction(
                user,
                AuditLogService.ACTION_PAYMENT_METHOD_UPDATED,
                paymentMethod.getId(),
                String.format("Sequence number güncellendi - Name: %s, Old: %d, New: %d", 
                        paymentMethod.getMethodName(), currentSequenceNumber, newSequenceNumber)
        );
        auditLogRepository.save(auditLog);
        
        log.info("Sequence number başarıyla güncellendi - ID: {}, Old: {}, New: {}", 
                paymentMethod.getId(), currentSequenceNumber, newSequenceNumber);
        
        return paymentMethod;
    }

    /**
     * Ödeme yöntemlerinin sequence number'larını yeniden düzenler
     * (Silme işlemlerinden sonra boşlukları doldurmak için)
     * 
     * @param userId Kullanıcı ID
     * @return Düzenlenen ödeme yöntemi sayısı
     */
    @Transactional
    public int reorderSequenceNumbers(UUID userId) {
        log.info("Sequence number'lar yeniden düzenleniyor - User: {}", userId);
        
        List<PaymentMethod> paymentMethods = paymentMethodRepository
                .findByUserIdAndIsActiveTrueOrderBySequenceNumberAsc(userId);
        
        int reorderedCount = 0;
        
        for (int i = 0; i < paymentMethods.size(); i++) {
            PaymentMethod method = paymentMethods.get(i);
            Integer expectedSequence = i + 1;
            
            if (!expectedSequence.equals(method.getSequenceNumber())) {
                method.setSequenceNumber(expectedSequence);
                method.setUpdatedAt(Instant.now());
                paymentMethodRepository.save(method);
                reorderedCount++;
                
                log.debug("Sequence number düzeltildi - Method: {}, Old: {}, New: {}", 
                        method.getId(), method.getSequenceNumber(), expectedSequence);
            }
        }
        
        log.info("Sequence number'lar başarıyla yeniden düzenlendi - User: {}, Total: {}, Reordered: {}", 
                userId, paymentMethods.size(), reorderedCount);
        
        return reorderedCount;
    }

    /**
     * Sequence number ile ödeme yöntemini günceller
     * 
     * @param userId Kullanıcı ID
     * @param sequenceNumber Sıra numarası
     * @param methodName Yeni ödeme yöntemi adı
     * @param isDefault Varsayılan ödeme yöntemi mi
     * @return Güncellenmiş PaymentMethod
     */
    @Transactional
    public PaymentMethod updatePaymentMethodBySequence(UUID userId, Integer sequenceNumber, String methodName, boolean isDefault) {
        log.info("Sequence number ile ödeme yöntemi güncelleniyor - User: {}, Sequence: {}, Name: {}", userId, sequenceNumber, methodName);
        
        // Sequence number ile ödeme yöntemini bul
        PaymentMethod paymentMethod = getPaymentMethodBySequence(userId, sequenceNumber);
        
        // Aynı isimde başka ödeme yöntemi var mı kontrol et (mevcut ödeme yöntemi hariç)
        if (paymentMethodRepository.existsByUserIdAndMethodNameAndIsActiveTrue(userId, methodName)) {
            PaymentMethod existingMethod = paymentMethodRepository
                    .findByUserIdAndMethodNameAndIsActiveTrue(userId, methodName)
                    .stream()
                    .findFirst()
                    .orElse(null);
            
            if (existingMethod != null && !existingMethod.getId().equals(paymentMethod.getId())) {
                throw new IllegalArgumentException("Bu isimde bir ödeme yöntemi zaten mevcut: " + methodName);
            }
        }
        
        // Eğer varsayılan olarak işaretleniyorsa, diğer varsayılanları kaldır
        if (isDefault) {
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
                String.format("Ödeme yöntemi güncellendi - Name: %s, Sequence: %d", methodName, sequenceNumber)
        );
        auditLogRepository.save(methodLog);
        
        log.info("Ödeme yöntemi başarıyla güncellendi - ID: {}, Name: {}", paymentMethod.getId(), methodName);
        
        return paymentMethod;
    }

    /**
     * PaymentMethod'un token'ını yeniler
     * 
     * @param userId Kullanıcı ID
     * @param sequenceNumber Sıra numarası
     * @param cardNumber Yeni kart numarası
     * @param cvv Yeni CVV
     */
    @Transactional
    public PaymentMethod refreshPaymentMethodToken(UUID userId, Integer sequenceNumber, String cardNumber, String cvv) {
        log.info("PaymentMethod token'ı yenileniyor - User: {}, Sequence: {}", userId, sequenceNumber);
        
        PaymentMethod paymentMethod = getPaymentMethodBySequence(userId, sequenceNumber);
        
        // Mevcut CardInfo'yu al
        CardInfo oldCardInfo = paymentMethod.getCardInfo();
        
        // Yeni token oluştur (aynı kart bilgileri ile)
        CardInfo newCardInfo = tokenizationService.tokenizeCard(
                cardNumber,
                oldCardInfo.getCardholderName(),
                oldCardInfo.getExpiryDate(),
                cvv
        );
        
        // PaymentMethod'u güncelle
        paymentMethod.setCardInfo(newCardInfo);
        paymentMethod.setUpdatedAt(Instant.now());
        
        paymentMethod = paymentMethodRepository.save(paymentMethod);
        
        // Eski token'ı geçersiz kıl
        if (oldCardInfo.getToken() != null) {
            tokenizationService.invalidateToken(oldCardInfo.getToken());
        }
        
        log.info("PaymentMethod token başarıyla yenilendi - ID: {}", paymentMethod.getId());
        return paymentMethod;
    }

    /**
     * Sequence number ile ödeme yöntemini siler
     * 
     * @param userId Kullanıcı ID
     * @param sequenceNumber Sıra numarası
     */
    @Transactional
    public void deletePaymentMethodBySequence(UUID userId, Integer sequenceNumber) {
        log.info("Sequence number ile ödeme yöntemi siliniyor - User: {}, Sequence: {}", userId, sequenceNumber);
        
        // Sequence number ile ödeme yöntemini bul
        PaymentMethod paymentMethod = getPaymentMethodBySequence(userId, sequenceNumber);
        
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
                String.format("Ödeme yöntemi silindi - Name: %s, Sequence: %d", paymentMethod.getMethodName(), sequenceNumber)
        );
        auditLogRepository.save(methodLog);
        
        // Sequence number'ları yeniden düzenle
        reorderSequenceNumbers(userId);
        
        log.info("Ödeme yöntemi başarıyla silindi - Sequence: {}", sequenceNumber);
    }
}
