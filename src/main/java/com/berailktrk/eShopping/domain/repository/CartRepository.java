package com.berailktrk.eShopping.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.berailktrk.eShopping.domain.model.Cart;
import com.berailktrk.eShopping.domain.model.User;

/**
 * Cart repository interface
 * Sepet yönetimi için repository
 */
@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    /**
     * Kullanıcının sepetini bul
     * 
     * @param user kullanıcı
     * @return sepet (varsa)
     */
    Optional<Cart> findByUser(User user);

    /**
     * Kullanıcı ID'ye göre sepet bul
     * 
     * @param userId kullanıcı ID
     * @return sepet (varsa)
     */
    @Query("SELECT c FROM Cart c WHERE c.user.id = :userId")
    Optional<Cart> findByUserId(@Param("userId") UUID userId);

    /**
     * Kullanıcının aktif sepetini bul
     * 
     * @param userId kullanıcı ID
     * @return aktif sepet (varsa)
     */
    @Query("SELECT c FROM Cart c WHERE c.user.id = :userId")
    Optional<Cart> findActiveCartByUserId(@Param("userId") UUID userId);

    /**
     * Kullanıcının sepet var mı kontrol et
     * 
     * @param userId kullanıcı ID
     * @return varsa true
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Cart c WHERE c.user.id = :userId")
    boolean existsByUserId(@Param("userId") UUID userId);

    /**
     * Eski sepetleri bul (belirli süreden önce güncellenmiş)
     * Cleanup işlemi için kullanılır
     * 
     * @param threshold zaman eşiği
     * @return eski sepetler
     */
    @Query("SELECT c FROM Cart c WHERE c.updatedAt < :threshold")
    List<Cart> findStaleActiveCarts(@Param("threshold") Instant threshold);

    /**
     * Kullanıcının sepetini temizle (tüm ürünleri kaldır)
     * 
     * @param userId kullanıcı ID
     */
    @Modifying
    @Query("UPDATE Cart c SET c.updatedAt = CURRENT_TIMESTAMP WHERE c.user.id = :userId")
    void updateCartTimestamp(@Param("userId") UUID userId);

    /**
     * Sepeti temizle (checkout sonrası - timestamp güncelleme)
     * 
     * @param cartId sepet ID
     */
    @Modifying
    @Query("UPDATE Cart c SET c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :cartId")
    void deactivateCart(@Param("cartId") UUID cartId);

    /**
     * Boş sepetleri bul
     * 
     * @return boş sepetler
     */
    @Query("SELECT c FROM Cart c WHERE NOT EXISTS (SELECT 1 FROM CartItem ci WHERE ci.cart.id = c.id)")
    List<Cart> findEmptyCarts();
}
