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

// Cart Repository - Sepet yönetimi işlemleri
@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    // Kullanıcının sepetini bul
    Optional<Cart> findByUser(User user);

    // Kullanıcı ID'ye göre sepet bul
    @Query("SELECT c FROM Cart c WHERE c.user.id = :userId")
    Optional<Cart> findByUserId(@Param("userId") UUID userId);

    // Kullanıcının aktif sepetini bul
    @Query("SELECT c FROM Cart c WHERE c.user.id = :userId")
    Optional<Cart> findActiveCartByUserId(@Param("userId") UUID userId);

    // Kullanıcının sepet var mı kontrol et
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Cart c WHERE c.user.id = :userId")
    boolean existsByUserId(@Param("userId") UUID userId);

    // Eski sepetleri bul - Cleanup işlemi için
    @Query("SELECT c FROM Cart c WHERE c.updatedAt < :threshold")
    List<Cart> findStaleActiveCarts(@Param("threshold") Instant threshold);

    // Kullanıcının sepet timestamp'ini güncelle
    @Modifying
    @Query("UPDATE Cart c SET c.updatedAt = CURRENT_TIMESTAMP WHERE c.user.id = :userId")
    void updateCartTimestamp(@Param("userId") UUID userId);

    // Sepeti deaktif et - Checkout sonrası timestamp güncelle
    @Modifying
    @Query("UPDATE Cart c SET c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :cartId")
    void deactivateCart(@Param("cartId") UUID cartId);

    // Boş sepetleri bul
    @Query("SELECT c FROM Cart c WHERE NOT EXISTS (SELECT 1 FROM CartItem ci WHERE ci.cart.id = c.id)")
    List<Cart> findEmptyCarts();
}
