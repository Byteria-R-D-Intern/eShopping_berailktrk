package com.berailktrk.eShopping.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.berailktrk.eShopping.domain.model.Cart;
import com.berailktrk.eShopping.domain.model.CartItem;

// CartItem Repository - Sepet kalemleri işlemleri
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    // Sepete ait tüm kalemleri getir
    List<CartItem> findByCart(Cart cart);

    // Sepet ID'ye göre kalemleri getir
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId")
    List<CartItem> findByCartId(@Param("cartId") UUID cartId);

    // Kullanıcının sepetindeki tüm kalemleri getir
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.user.id = :userId")
    List<CartItem> findByUserId(@Param("userId") UUID userId);

    // Sepet ve ürüne göre kalem bul
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.product.id = :productId")
    Optional<CartItem> findByCartIdAndProductId(@Param("cartId") UUID cartId, @Param("productId") UUID productId);

    // Sepetteki toplam ürün sayısını hesapla - Miktar toplamı
    @Query("SELECT COALESCE(SUM(ci.qty), 0) FROM CartItem ci WHERE ci.cart.id = :cartId")
    Long getTotalQuantityByCartId(@Param("cartId") UUID cartId);

    // Sepetteki farklı ürün sayısını getir
    @Query("SELECT COUNT(ci) FROM CartItem ci WHERE ci.cart.id = :cartId")
    Long countByCartId(@Param("cartId") UUID cartId);

    // Sepet ID'ye göre tüm kalemleri sil
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    void deleteByCartId(@Param("cartId") UUID cartId);

    // Belirli bir ürünü sepetten sil
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.product.id = :productId")
    void deleteByCartIdAndProductId(@Param("cartId") UUID cartId, @Param("productId") UUID productId);

    // Sepet kalemi miktarını güncelle
    @Modifying
    @Query("UPDATE CartItem ci SET ci.qty = :quantity WHERE ci.id = :cartItemId")
    void updateQuantity(@Param("cartItemId") UUID cartItemId, @Param("quantity") Integer quantity);

    // Aktif olmayan ürünleri sepetten temizle
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.product.isActive = false")
    void removeInactiveProducts(@Param("cartId") UUID cartId);
}
