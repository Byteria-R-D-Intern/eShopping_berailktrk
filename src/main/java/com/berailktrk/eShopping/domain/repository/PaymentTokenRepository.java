package com.berailktrk.eShopping.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.berailktrk.eShopping.domain.model.PaymentToken;

// PaymentToken Repository - Ödeme token yönetimi
// GÜVENLİK NOTU: Dönen veriler maskelenmeli (sadece last4 ve expiry gösterilmeli)
@Repository
public interface PaymentTokenRepository extends JpaRepository<PaymentToken, UUID> {

    // Kullanıcının tüm ödeme tokenlerini getir
    @Query("SELECT pt FROM PaymentToken pt WHERE pt.user.id = :userId ORDER BY pt.createdAt DESC")
    List<PaymentToken> findByUserId(@Param("userId") UUID userId);

    // Kullanıcının aktif tokenlerini getir
    @Query("SELECT pt FROM PaymentToken pt WHERE pt.user.id = :userId ORDER BY pt.createdAt DESC")
    List<PaymentToken> findActiveTokensByUserId(@Param("userId") UUID userId);

    // Provider token'e göre token bul
    Optional<PaymentToken> findByProviderToken(String providerToken);

    // Kullanıcı ve provider token'e göre bul
    @Query("SELECT pt FROM PaymentToken pt WHERE pt.user.id = :userId AND pt.providerToken = :providerToken")
    Optional<PaymentToken> findByUserIdAndProviderToken(@Param("userId") UUID userId, @Param("providerToken") String providerToken);

    // Kullanıcının varsayılan ödeme yöntemini getir
    @Query("SELECT pt FROM PaymentToken pt WHERE pt.user.id = :userId AND pt.isDefault = true")
    Optional<PaymentToken> findDefaultTokenByUserId(@Param("userId") UUID userId);

    // Kullanıcının tokenlerinin varsayılan işaretini kaldır - Yeni default ayarlamadan önce
    @Modifying
    @Query("UPDATE PaymentToken pt SET pt.isDefault = false WHERE pt.user.id = :userId")
    void clearDefaultTokens(@Param("userId") UUID userId);

    // Süresi dolmuş tokenleri bul
    @Query("SELECT pt FROM PaymentToken pt WHERE (pt.expiryYear < :currentYear) OR (pt.expiryYear = :currentYear AND pt.expiryMonth < :currentMonth)")
    List<PaymentToken> findExpiredTokens(@Param("currentYear") Integer currentYear, @Param("currentMonth") Integer currentMonth);

    // Tokeni sil
    @Modifying
    @Query("DELETE FROM PaymentToken pt WHERE pt.id = :tokenId")
    void deleteToken(@Param("tokenId") UUID tokenId);

    // Kullanıcının token sayısını getir
    @Query("SELECT COUNT(pt) FROM PaymentToken pt WHERE pt.user.id = :userId")
    Long countTokensByUserId(@Param("userId") UUID userId);

    // Last4 ve kullanıcıya göre token bul - Duplicate kontrolü için
    @Query("SELECT pt FROM PaymentToken pt WHERE pt.user.id = :userId AND pt.last4 = :last4")
    List<PaymentToken> findByUserIdAndLast4(@Param("userId") UUID userId, @Param("last4") String last4);
}
