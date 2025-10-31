package com.berailktrk.eShopping.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.berailktrk.eShopping.domain.model.PaymentMethod;


//PaymentMethod entity için repository interface

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {

    //Belirli bir kullanıcıya ait ödeme yöntemlerini getir
    
    List<PaymentMethod> findByUserIdAndIsActiveTrueOrderByIsDefaultDescCreatedAtDesc(UUID userId);

    //Belirli bir kullanıcının varsayılan ödeme yöntemini getir
    
    Optional<PaymentMethod> findByUserIdAndIsDefaultTrueAndIsActiveTrue(UUID userId);

    //Belirli bir kullanıcıya ait aktif ödeme yöntemlerini getir
    
    List<PaymentMethod> findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(UUID userId);

    //Belirli bir kullanıcının belirli bir türdeki ödeme yöntemlerini getir
    
    List<PaymentMethod> findByUserIdAndMethodTypeAndIsActiveTrueOrderByCreatedAtDesc(UUID userId, String methodType);

    //Belirli bir kullanıcının ödeme yöntemi sayısını getir
    
    long countByUserIdAndIsActiveTrue(UUID userId);

    //Belirli bir kullanıcının varsayılan ödeme yöntemi var mı kontrol et
    
    boolean existsByUserIdAndIsDefaultTrueAndIsActiveTrue(UUID userId);

    //Belirli bir kullanıcının belirli bir ödeme yöntemi adına sahip kaydı var mı kontrol et
    
    boolean existsByUserIdAndMethodNameAndIsActiveTrue(UUID userId, String methodName);

    //Belirli bir kullanıcının tüm varsayılan ödeme yöntemlerini false yap
    //(Yeni varsayılan ödeme yöntemi eklenirken kullanılır)
    
    @Modifying(clearAutomatically = true)
    @Query("UPDATE PaymentMethod pm SET pm.isDefault = false WHERE pm.user.id = :userId AND pm.isDefault = true")
    void clearDefaultPaymentMethods(@Param("userId") UUID userId);

    // ==================== SEQUENCE NUMBER İŞLEMLERİ ====================

    //Belirli bir kullanıcının ödeme yöntemlerini sequence number'a göre sıralı getir
    
    List<PaymentMethod> findByUserIdAndIsActiveTrueOrderBySequenceNumberAsc(UUID userId);

    //Belirli bir kullanıcının belirli sequence number'ına sahip ödeme yöntemini getir
    
    Optional<PaymentMethod> findByUserIdAndSequenceNumberAndIsActiveTrue(UUID userId, Integer sequenceNumber);

    //Belirli bir kullanıcının en yüksek sequence number'ını getir
    
    @Query("SELECT COALESCE(MAX(pm.sequenceNumber), 0) FROM PaymentMethod pm WHERE pm.user.id = :userId AND pm.isActive = true")
    Integer findMaxSequenceNumberByUserId(@Param("userId") UUID userId);

    //Belirli bir kullanıcının belirli sequence number'ına sahip ödeme yöntemi var mı kontrol et
    
    boolean existsByUserIdAndSequenceNumberAndIsActiveTrue(UUID userId, Integer sequenceNumber);

    //Belirli bir kullanıcının belirli bir ödeme yöntemi adına sahip kaydı var mı kontrol et (Liste döner)
    
    List<PaymentMethod> findByUserIdAndMethodNameAndIsActiveTrue(UUID userId, String methodName);
}
