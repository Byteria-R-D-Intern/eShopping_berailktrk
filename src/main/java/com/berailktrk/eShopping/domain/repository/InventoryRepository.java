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

// Inventory Repository - Stok yönetimi ve concurrency kontrolü
// ÖNEMLİ: Race condition'ları önlemek için pessimistic locking kullanılır
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    // Ürüne göre stok kaydı bul
    Optional<Inventory> findByProduct(Product product);

    // Düşük stoklu ürünleri getir - Threshold altında
    @Query("SELECT i FROM Inventory i WHERE i.quantity <= :threshold AND i.product.isActive = true")
    List<Inventory> findLowStockItems(@Param("threshold") Integer threshold);

    // Tükenen stokları getir - Quantity = 0
    @Query("SELECT i FROM Inventory i WHERE i.quantity = 0 AND i.product.isActive = true")
    List<Inventory> findOutOfStockItems();

    // ==================== SKU-BASED METHODS ====================

    // SKU'ya göre stok kaydı bul
    @Query("SELECT i FROM Inventory i WHERE i.productSku = :sku")
    Optional<Inventory> findByProductSku(@Param("sku") String sku);

    // SKU'ya göre stok kaydını pessimistic lock ile getir
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productSku = :sku")
    Optional<Inventory> findByProductSkuWithLock(@Param("sku") String sku);

    // SKU'ya göre stok miktarını artır/azalt (delta değeri)
    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = i.quantity + :delta, " +
           "i.version = i.version + 1 " +
           "WHERE i.productSku = :sku")
    int adjustStockBySku(@Param("sku") String sku, @Param("delta") Integer delta);

    // SKU'ya göre stok miktarını azalt - Rezervasyon için (optimistic locking)
    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = i.quantity - :quantity, " +
           "i.reserved = i.reserved + :quantity, " +
           "i.version = i.version + 1 " +
           "WHERE i.productSku = :sku AND i.quantity >= :quantity AND i.version = :currentVersion")
    int decreaseStockBySku(@Param("sku") String sku, 
                           @Param("quantity") Integer quantity, 
                           @Param("currentVersion") Integer currentVersion);

    // SKU'ya göre rezervasyonu onayla - Reserved'dan düş
    @Modifying
    @Query("UPDATE Inventory i SET i.reserved = i.reserved - :quantity, " +
           "i.version = i.version + 1 " +
           "WHERE i.productSku = :sku AND i.reserved >= :quantity AND i.version = :currentVersion")
    int confirmReservationBySku(@Param("sku") String sku, 
                                @Param("quantity") Integer quantity, 
                                @Param("currentVersion") Integer currentVersion);

    // SKU'ya göre rezervasyonu iptal et - Quantity'ye geri ekle
    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = i.quantity + :quantity, " +
           "i.reserved = i.reserved - :quantity, " +
           "i.version = i.version + 1 " +
           "WHERE i.productSku = :sku AND i.reserved >= :quantity AND i.version = :currentVersion")
    int cancelReservationBySku(@Param("sku") String sku, 
                               @Param("quantity") Integer quantity, 
                               @Param("currentVersion") Integer currentVersion);
}
