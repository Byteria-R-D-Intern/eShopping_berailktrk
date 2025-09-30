package com.berailktrk.eShopping.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.berailktrk.eShopping.domain.model.Inventory;
import com.berailktrk.eShopping.domain.model.Product;

import jakarta.persistence.LockModeType;

/**
 * Inventory repository interface
 * Stok yönetimi ve concurrency kontrolü için repository
 * 
 * ÖNEMLİ: Stok güncellemeleri için pessimistic locking kullanılır
 * Race condition'ları önlemek için kritik işlemlerde lock kullanın
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    /**
     * Ürüne göre stok kaydı bul
     * 
     * @param product ürün
     * @return stok kaydı (varsa)
     */
    Optional<Inventory> findByProduct(Product product);

    /**
     * Ürün ID'ye göre stok kaydı bul
     * 
     * @param productId ürün ID
     * @return stok kaydı (varsa)
     */
    @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId")
    Optional<Inventory> findByProductId(@Param("productId") UUID productId);

    /**
     * Ürün ID'ye göre stok kaydını pessimistic lock ile getir
     * Concurrency kontrolü için kullanılır
     * 
     * @param productId ürün ID
     * @return stok kaydı (varsa)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId")
    Optional<Inventory> findByProductIdWithLock(@Param("productId") UUID productId);

    /**
     * Düşük stoklu ürünleri getir (threshold altında)
     * 
     * @param threshold eşik değeri
     * @return düşük stoklu ürünler
     */
    @Query("SELECT i FROM Inventory i WHERE i.quantity <= :threshold AND i.product.isActive = true")
    List<Inventory> findLowStockItems(@Param("threshold") Integer threshold);

    /**
     * Tükenen stokları getir
     * 
     * @return stok tükenmiş ürünler
     */
    @Query("SELECT i FROM Inventory i WHERE i.quantity = 0 AND i.product.isActive = true")
    List<Inventory> findOutOfStockItems();

    /**
     * Stok miktarını azalt (atomic operation with version check)
     * 
     * @param productId ürün ID
     * @param quantity azaltılacak miktar
     * @param currentVersion mevcut version (optimistic locking için)
     * @return etkilenen satır sayısı
     */
    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = i.quantity - :quantity, " +
           "i.reserved = i.reserved + :quantity, " +
           "i.version = i.version + 1 " +
           "WHERE i.product.id = :productId AND i.quantity >= :quantity AND i.version = :currentVersion")
    int decreaseStock(@Param("productId") UUID productId, 
                      @Param("quantity") Integer quantity, 
                      @Param("currentVersion") Integer currentVersion);

    /**
     * Reserve edilen stoku onaylayıp available'dan düş
     * 
     * @param productId ürün ID
     * @param quantity miktar
     * @param currentVersion mevcut version
     * @return etkilenen satır sayısı
     */
    @Modifying
    @Query("UPDATE Inventory i SET i.reserved = i.reserved - :quantity, " +
           "i.version = i.version + 1 " +
           "WHERE i.product.id = :productId AND i.reserved >= :quantity AND i.version = :currentVersion")
    int confirmReservation(@Param("productId") UUID productId, 
                           @Param("quantity") Integer quantity, 
                           @Param("currentVersion") Integer currentVersion);

    /**
     * Reserve edilen stoku iptal et ve geri döndür
     * 
     * @param productId ürün ID
     * @param quantity miktar
     * @param currentVersion mevcut version
     * @return etkilenen satır sayısı
     */
    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = i.quantity + :quantity, " +
           "i.reserved = i.reserved - :quantity, " +
           "i.version = i.version + 1 " +
           "WHERE i.product.id = :productId AND i.reserved >= :quantity AND i.version = :currentVersion")
    int cancelReservation(@Param("productId") UUID productId, 
                          @Param("quantity") Integer quantity, 
                          @Param("currentVersion") Integer currentVersion);

    /**
     * Stok miktarını artır (iade veya stok ekleme için)
     * 
     * @param productId ürün ID
     * @param quantity artırılacak miktar
     * @return etkilenen satır sayısı
     */
    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = i.quantity + :quantity, " +
           "i.version = i.version + 1 " +
           "WHERE i.product.id = :productId")
    int increaseStock(@Param("productId") UUID productId, @Param("quantity") Integer quantity);
}
