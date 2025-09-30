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

/**
 * PaymentToken repository interface
 * Ödeme token yönetimi için repository
 * 
 * GÜVENLİK NOTU: Bu repository'den dönen veriler kullanıcıya gösterilmeden önce
 * maskelenmeli (sadece last4 ve expiry gösterilmeli)
 */
@Repository
public interface PaymentTokenRepository extends JpaRepository<PaymentToken, UUID> {

    /**
     * Kullanıcının tüm ödeme tokenlerini getir
     * 
     * @param userId kullanıcı ID
     * @return ödeme tokenleri
     */
    @Query("SELECT pt FROM PaymentToken pt WHERE pt.user.id = :userId ORDER BY pt.createdAt DESC")
    List<PaymentToken> findByUserId(@Param("userId") UUID userId);

    /**
     * Kullanıcının aktif tokenlerini getir
     * 
     * @param userId kullanıcı ID
     * @return ödeme tokenleri
     */
    @Query("SELECT pt FROM PaymentToken pt WHERE pt.user.id = :userId ORDER BY pt.createdAt DESC")
    List<PaymentToken> findActiveTokensByUserId(@Param("userId") UUID userId);

    /**
     * Provider token'e göre token bul
     * 
     * @param providerToken token değeri
     * @return ödeme tokeni (varsa)
     */
    Optional<PaymentToken> findByProviderToken(String providerToken);

    /**
     * Kullanıcı ve provider token'e göre bul
     * 
     * @param userId kullanıcı ID
     * @param providerToken token değeri
     * @return ödeme tokeni (varsa)
     */
    @Query("SELECT pt FROM PaymentToken pt WHERE pt.user.id = :userId AND pt.providerToken = :providerToken")
    Optional<PaymentToken> findByUserIdAndProviderToken(@Param("userId") UUID userId, @Param("providerToken") String providerToken);

    /**
     * Kullanıcının varsayılan ödeme yöntemini getir
     * 
     * @param userId kullanıcı ID
     * @return varsayılan ödeme tokeni (varsa)
     */
    @Query("SELECT pt FROM PaymentToken pt WHERE pt.user.id = :userId AND pt.isDefault = true")
    Optional<PaymentToken> findDefaultTokenByUserId(@Param("userId") UUID userId);

    /**
     * Kullanıcının tokenlerinin varsayılan işaretini kaldır
     * Yeni varsayılan token ayarlamadan önce kullanılır
     * 
     * @param userId kullanıcı ID
     */
    @Modifying
    @Query("UPDATE PaymentToken pt SET pt.isDefault = false WHERE pt.user.id = :userId")
    void clearDefaultTokens(@Param("userId") UUID userId);

    /**
     * Süresi dolmuş tokenleri bul
     * 
     * @param currentYear şu anki yıl
     * @param currentMonth şu anki ay
     * @return süresi dolmuş tokenler
     */
    @Query("SELECT pt FROM PaymentToken pt WHERE (pt.expiryYear < :currentYear) OR (pt.expiryYear = :currentYear AND pt.expiryMonth < :currentMonth)")
    List<PaymentToken> findExpiredTokens(@Param("currentYear") Integer currentYear, @Param("currentMonth") Integer currentMonth);

    /**
     * Tokeni sil
     * 
     * @param tokenId token ID
     */
    @Modifying
    @Query("DELETE FROM PaymentToken pt WHERE pt.id = :tokenId")
    void deleteToken(@Param("tokenId") UUID tokenId);

    /**
     * Kullanıcının token sayısını getir
     * 
     * @param userId kullanıcı ID
     * @return token sayısı
     */
    @Query("SELECT COUNT(pt) FROM PaymentToken pt WHERE pt.user.id = :userId")
    Long countTokensByUserId(@Param("userId") UUID userId);

    /**
     * Last4 ve kullanıcıya göre token bul (duplicate kontrolü için)
     * 
     * @param userId kullanıcı ID
     * @param last4 son 4 rakam
     * @return bulunan tokenler
     */
    @Query("SELECT pt FROM PaymentToken pt WHERE pt.user.id = :userId AND pt.last4 = :last4")
    List<PaymentToken> findByUserIdAndLast4(@Param("userId") UUID userId, @Param("last4") String last4);
}
