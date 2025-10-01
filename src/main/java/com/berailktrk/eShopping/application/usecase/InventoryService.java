package com.berailktrk.eShopping.application.usecase;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.berailktrk.eShopping.domain.model.Inventory;
import com.berailktrk.eShopping.domain.model.Product;
import com.berailktrk.eShopping.domain.repository.InventoryRepository;
import com.berailktrk.eShopping.domain.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Inventory business logic service
 * Stok yönetimi, rezervasyon ve concurrency kontrolü
 * 
 * ÖNEMLİ: Tüm stok işlemleri transactional olmalı
 * Race condition'ları önlemek için optimistic/pessimistic locking kullanılır
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;


    /**
     * Düşük stoklu ürünleri getir
     * 
     * @param threshold eşik değeri
     * @return düşük stoklu ürünler
     */
    @Transactional(readOnly = true)
    public List<Inventory> getLowStockItems(Integer threshold) {
        log.info("Getting low stock items with threshold: {}", threshold);
        return inventoryRepository.findLowStockItems(threshold);
    }

    /**
     * Stok tükenmiş ürünleri getir
     * 
     * @return stok tükenmiş ürünler
     */
    @Transactional(readOnly = true)
    public List<Inventory> getOutOfStockItems() {
        log.info("Getting out of stock items");
        return inventoryRepository.findOutOfStockItems();
    }

    // ==================== SKU-BASED METHODS ====================

    /**
     * Stok kaydı oluştur
     * 
     * @param sku ürün SKU
     * @param initialQuantity başlangıç stok miktarı
     * @return oluşturulan stok kaydı
     */
    public Inventory createInventory(String sku, Integer initialQuantity) {
        log.info("Creating inventory for SKU: {} with quantity: {}", sku, initialQuantity);
        
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with SKU: " + sku));

        if (inventoryRepository.findByProductSku(sku).isPresent()) {
            throw new IllegalArgumentException("Inventory already exists for SKU: " + sku);
        }

        Inventory inventory = Inventory.builder()
                .product(product)
                .quantity(initialQuantity)
                .reserved(0)
                .updatedAt(Instant.now())
                .version(0)
                .build();

        return inventoryRepository.save(inventory);
    }

    /**
     * Stok miktarını artır
     * 
     * @param sku ürün SKU
     * @param quantity artırılacak miktar
     * @return güncellenen stok kaydı
     */
    public Inventory increaseStock(String sku, Integer quantity) {
        log.info("Increasing stock for SKU: {} by quantity: {}", sku, quantity);
        
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        Inventory inventory = getInventoryBySku(sku);
        
        int updatedRows = inventoryRepository.increaseStockBySku(sku, quantity);
        if (updatedRows == 0) {
            throw new IllegalStateException("Failed to increase stock for SKU: " + sku);
        }

        // Refresh entity to get updated data
        inventoryRepository.flush();
        return inventoryRepository.findByProductSku(sku)
                .orElseThrow(() -> new IllegalStateException("Inventory not found after update: " + sku));
    }

    /**
     * Stok rezervasyonu yap
     * 
     * @param sku ürün SKU
     * @param quantity rezerve edilecek miktar
     * @return rezervasyon başarılı mı
     */
    @Transactional
    public boolean reserveStock(String sku, Integer quantity) {
        log.info("Reserving stock for SKU: {} quantity: {}", sku, quantity);
        
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        // Pessimistic lock ile stok kaydını getir
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductSkuWithLock(sku);
        if (inventoryOpt.isEmpty()) {
            throw new IllegalArgumentException("Inventory not found for SKU: " + sku);
        }

        Inventory inventory = inventoryOpt.get();
        
        // Stok yeterli mi kontrol et
        if (inventory.getQuantity() < quantity) {
            log.warn("Insufficient stock for SKU: {} requested: {} available: {}", 
                    sku, quantity, inventory.getQuantity());
            return false;
        }

        // Optimistic locking ile stok azalt ve rezerve et
        int updatedRows = inventoryRepository.decreaseStockBySku(
                sku, 
                quantity, 
                inventory.getVersion()
        );

        if (updatedRows == 0) {
            log.warn("Failed to reserve stock for SKU: {} (version conflict)", sku);
            return false;
        }

        log.info("Successfully reserved {} units for SKU: {}", quantity, sku);
        return true;
    }

    /**
     * Rezervasyonu onayla
     * 
     * @param sku ürün SKU
     * @param quantity onaylanacak miktar
     * @return onay başarılı mı
     */
    @Transactional
    public boolean confirmReservation(String sku, Integer quantity) {
        log.info("Confirming reservation for SKU: {} quantity: {}", sku, quantity);
        
        Inventory inventory = getInventoryBySku(sku);
        
        if (inventory.getReserved() < quantity) {
            throw new IllegalArgumentException("Insufficient reserved stock for SKU: " + sku);
        }

        int updatedRows = inventoryRepository.confirmReservationBySku(
                sku, 
                quantity, 
                inventory.getVersion()
        );

        if (updatedRows == 0) {
            throw new IllegalStateException("Failed to confirm reservation for SKU: " + sku);
        }

        log.info("Successfully confirmed reservation for SKU: {} quantity: {}", sku, quantity);
        return true;
    }

    /**
     * Rezervasyonu iptal et
     * 
     * @param sku ürün SKU
     * @param quantity iptal edilecek miktar
     * @return iptal başarılı mı
     */
    @Transactional
    public boolean cancelReservation(String sku, Integer quantity) {
        log.info("Cancelling reservation for SKU: {} quantity: {}", sku, quantity);
        
        Inventory inventory = getInventoryBySku(sku);
        
        if (inventory.getReserved() < quantity) {
            throw new IllegalArgumentException("Insufficient reserved stock for SKU: " + sku);
        }

        int updatedRows = inventoryRepository.cancelReservationBySku(
                sku, 
                quantity, 
                inventory.getVersion()
        );

        if (updatedRows == 0) {
            throw new IllegalStateException("Failed to cancel reservation for SKU: " + sku);
        }

        log.info("Successfully cancelled reservation for SKU: {} quantity: {}", sku, quantity);
        return true;
    }

    /**
     * Stok bilgilerini getir
     * 
     * @param sku ürün SKU
     * @return stok bilgileri
     */
    @Transactional(readOnly = true)
    public Inventory getInventoryBySku(String sku) {
        return inventoryRepository.findByProductSku(sku)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found for SKU: " + sku));
    }

    /**
     * Stok durumunu kontrol et
     * 
     * @param sku ürün SKU
     * @param requestedQuantity istenen miktar
     * @return stok yeterli mi
     */
    @Transactional(readOnly = true)
    public boolean isStockAvailable(String sku, Integer requestedQuantity) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductSku(sku);
        if (inventoryOpt.isEmpty()) {
            return false;
        }

        Inventory inventory = inventoryOpt.get();
        return inventory.getQuantity() >= requestedQuantity;
    }

    /**
     * Mevcut stok miktarını getir
     * 
     * @param sku ürün SKU
     * @return mevcut stok miktarı
     */
    @Transactional(readOnly = true)
    public Integer getAvailableStock(String sku) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductSku(sku);
        if (inventoryOpt.isEmpty()) {
            return 0;
        }

        return inventoryOpt.get().getQuantity();
    }
}