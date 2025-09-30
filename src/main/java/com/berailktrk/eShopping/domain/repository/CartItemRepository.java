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

/**
 * CartItem repository interface
 * Sepet kalemleri için repository
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    /**
     * Sepete ait tüm kalemleri getir
     * 
     * @param cart sepet
     * @return sepet kalemleri
     */
    List<CartItem> findByCart(Cart cart);

    /**
     * Sepet ID'ye göre kalemleri getir
     * 
     * @param cartId sepet ID
     * @return sepet kalemleri
     */
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId")
    List<CartItem> findByCartId(@Param("cartId") UUID cartId);

    /**
     * Kullanıcının sepetindeki tüm kalemleri getir
     * 
     * @param userId kullanıcı ID
     * @return sepet kalemleri
     */
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.user.id = :userId")
    List<CartItem> findByUserId(@Param("userId") UUID userId);

    /**
     * Sepet ve ürüne göre kalem bul
     * 
     * @param cartId sepet ID
     * @param productId ürün ID
     * @return sepet kalemi (varsa)
     */
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.product.id = :productId")
    Optional<CartItem> findByCartIdAndProductId(@Param("cartId") UUID cartId, @Param("productId") UUID productId);

    /**
     * Sepetteki toplam ürün sayısını hesapla
     * 
     * @param cartId sepet ID
     * @return toplam ürün sayısı (miktar toplamı)
     */
    @Query("SELECT COALESCE(SUM(ci.qty), 0) FROM CartItem ci WHERE ci.cart.id = :cartId")
    Long getTotalQuantityByCartId(@Param("cartId") UUID cartId);

    /**
     * Sepetteki farklı ürün sayısını getir
     * 
     * @param cartId sepet ID
     * @return farklı ürün sayısı
     */
    @Query("SELECT COUNT(ci) FROM CartItem ci WHERE ci.cart.id = :cartId")
    Long countByCartId(@Param("cartId") UUID cartId);

    /**
     * Sepet ID'ye göre tüm kalemleri sil
     * 
     * @param cartId sepet ID
     */
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    void deleteByCartId(@Param("cartId") UUID cartId);

    /**
     * Belirli bir ürünü sepetten sil
     * 
     * @param cartId sepet ID
     * @param productId ürün ID
     */
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.product.id = :productId")
    void deleteByCartIdAndProductId(@Param("cartId") UUID cartId, @Param("productId") UUID productId);

    /**
     * Sepet kalemi miktarını güncelle
     * 
     * @param cartItemId sepet kalemi ID
     * @param quantity yeni miktar
     */
    @Modifying
    @Query("UPDATE CartItem ci SET ci.qty = :quantity WHERE ci.id = :cartItemId")
    void updateQuantity(@Param("cartItemId") UUID cartItemId, @Param("quantity") Integer quantity);

    /**
     * Aktif olmayan ürünleri sepetten temizle
     * 
     * @param cartId sepet ID
     */
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.product.isActive = false")
    void removeInactiveProducts(@Param("cartId") UUID cartId);
}
