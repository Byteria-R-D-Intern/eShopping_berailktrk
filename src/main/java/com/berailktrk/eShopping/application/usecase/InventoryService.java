package com.berailktrk.eShopping.application.usecase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.berailktrk.eShopping.domain.model.AuditLog;
import com.berailktrk.eShopping.domain.model.Inventory;
import com.berailktrk.eShopping.domain.model.Product;
import com.berailktrk.eShopping.domain.model.User;
import com.berailktrk.eShopping.domain.repository.AuditLogRepository;
import com.berailktrk.eShopping.domain.repository.InventoryRepository;
import com.berailktrk.eShopping.domain.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//Inventory service - stok yönetimi, rezervasyon ve concurrency kontrolü
//Transactional işlemler, optimistic/pessimistic locking
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final AuditLogService auditLogService;
    private final AuditLogRepository auditLogRepository;


    //Düşük stoklu ürünleri getir
    @Transactional(readOnly = true)
    public List<Inventory> getLowStockItems(Integer threshold) {
        log.info("Getting low stock items with threshold: {}", threshold);
        return inventoryRepository.findLowStockItems(threshold);
    }

    //Stok tükenmiş ürünleri getir
    @Transactional(readOnly = true)
    public List<Inventory> getOutOfStockItems() {
        log.info("Getting out of stock items");
        return inventoryRepository.findOutOfStockItems();
    }

    //SKU-based metodlar

    //Stok kaydı oluştur
    public Inventory createInventory(String sku, Integer initialQuantity) {
        log.info("Creating inventory for SKU: {} with quantity: {}", sku, initialQuantity);
        
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with SKU: " + sku));

        if (inventoryRepository.findByProductSku(sku).isPresent()) {
            throw new IllegalArgumentException("Inventory already exists for SKU: " + sku);
        }

        Inventory inventory = Inventory.builder()
                .product(product)
                .productId(product.getId())
                .quantity(initialQuantity)
                .reserved(0)
                .build();

        return inventoryRepository.save(inventory);
    }

    //Stok miktarını artır veya azalt (pozitif: artır, negatif: azalt)
    public Inventory adjustStock(String sku, Integer delta, User actorUser) {
        log.info("Adjusting stock for SKU: {} by delta: {}", sku, delta);
        
        if (delta == 0) {
            throw new IllegalArgumentException("Delta cannot be zero");
        }

        //SKU'nun var olup olmadığını ve mevcut stok kontrolü
        Inventory currentInventory = getInventoryBySku(sku);
        
        //BEFORE değerleri
        Map<String, Object> beforeValues = new HashMap<>();
        beforeValues.put("quantity", currentInventory.getQuantity());
        beforeValues.put("reserved", currentInventory.getReserved());
        
        //Negatif delta ise, stokta yeterli miktar olup olmadığını kontrol et
        if (delta < 0 && currentInventory.getQuantity() + delta < 0) {
            throw new IllegalArgumentException(
                String.format("Insufficient stock. Current: %d, Requested delta: %d", 
                    currentInventory.getQuantity(), delta));
        }
        
        int updatedRows = inventoryRepository.adjustStockBySku(sku, delta);
        if (updatedRows == 0) {
            throw new IllegalStateException("Failed to adjust stock for SKU: " + sku);
        }

        //Refresh entity to get updated data
        inventoryRepository.flush();
        Inventory updatedInventory = inventoryRepository.findByProductSku(sku)
                .orElseThrow(() -> new IllegalStateException("Inventory not found after update: " + sku));
        
        //AFTER değerleri
        Map<String, Object> afterValues = new HashMap<>();
        afterValues.put("quantity", updatedInventory.getQuantity());
        afterValues.put("reserved", updatedInventory.getReserved());
        
        //Detaylı audit log oluştur
        Map<String, Object> details = new HashMap<>();
        details.put("before", beforeValues);
        details.put("after", afterValues);
        details.put("delta", delta);
        details.put("sku", sku);
        details.put("reason", delta > 0 ? "Stok artırıldı" : "Stok azaltıldı");
        
        AuditLog inventoryLog = auditLogService.logInventoryAction(
            actorUser,
            AuditLogService.ACTION_INVENTORY_UPDATED,
            currentInventory.getProduct().getId(),
            String.format("Stok ayarlandı: %s, Delta: %d", sku, delta),
            details
        );
        auditLogRepository.save(inventoryLog);
        
        return updatedInventory;
    }

    //Stok rezervasyonu yap
    @Transactional
    public boolean reserveStock(String sku, Integer quantity, User actorUser) {
        log.info("Reserving stock for SKU: {} quantity: {}", sku, quantity);
        
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        //Pessimistic lock ile stok kaydını getir
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductSkuWithLock(sku);
        if (inventoryOpt.isEmpty()) {
            throw new IllegalArgumentException("Inventory not found for SKU: " + sku);
        }

        Inventory inventory = inventoryOpt.get();
        
        //BEFORE değerleri
        Map<String, Object> beforeValues = new HashMap<>();
        beforeValues.put("quantity", inventory.getQuantity());
        beforeValues.put("reserved", inventory.getReserved());
        
        //Stok yeterli mi kontrol et
        if (inventory.getQuantity() < quantity) {
            log.warn("Insufficient stock for SKU: {} requested: {} available: {}", 
                    sku, quantity, inventory.getQuantity());
            
            //Başarısız rezervasyon audit log
            Map<String, Object> details = new HashMap<>();
            details.put("before", beforeValues);
            details.put("requested_quantity", quantity);
            details.put("sku", sku);
            details.put("reason", "Yetersiz stok");
            
            AuditLog failLog = auditLogService.logInventoryAction(
                actorUser,
                "INVENTORY_RESERVATION_FAILED",
                inventory.getProduct().getId(),
                String.format("Stok rezervasyonu başarısız: %s, İstenen: %d, Mevcut: %d", 
                    sku, quantity, inventory.getQuantity()),
                details
            );
            auditLogRepository.save(failLog);
            
            return false;
        }

        //Optimistic locking ile stok azalt ve rezerve et
        int updatedRows = inventoryRepository.decreaseStockBySku(
                sku, 
                quantity, 
                inventory.getVersion()
        );

        if (updatedRows == 0) {
            log.warn("Failed to reserve stock for SKU: {} (version conflict)", sku);
            
            //Version conflict audit log
            Map<String, Object> details = new HashMap<>();
            details.put("before", beforeValues);
            details.put("requested_quantity", quantity);
            details.put("sku", sku);
            details.put("reason", "Version conflict");
            
            AuditLog conflictLog = auditLogService.logInventoryAction(
                actorUser,
                "INVENTORY_RESERVATION_FAILED",
                inventory.getProduct().getId(),
                String.format("Stok rezervasyonu başarısız: %s (version conflict)", sku),
                details
            );
            auditLogRepository.save(conflictLog);
            
            return false;
        }

        //Başarılı rezervasyon audit log
        Map<String, Object> details = new HashMap<>();
        details.put("before", beforeValues);
        details.put("reserved_quantity", quantity);
        details.put("sku", sku);
        details.put("reason", "Stok rezerve edildi");
        
        AuditLog reserveLog = auditLogService.logInventoryAction(
            actorUser,
            AuditLogService.ACTION_INVENTORY_STOCK_RESERVED,
            inventory.getProductId(),
            String.format("Stok rezerve edildi: %s, Miktar: %d", sku, quantity),
            details
        );
        auditLogRepository.save(reserveLog);

        log.info("Successfully reserved {} units for SKU: {}", quantity, sku);
        return true;
    }

    //Rezervasyonu onayla
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

    //Rezervasyonu iptal et
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

    //Stok bilgilerini getir
    @Transactional(readOnly = true)
    public Inventory getInventoryBySku(String sku) {
        return inventoryRepository.findByProductSku(sku)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found for SKU: " + sku));
    }

    //Stok durumunu kontrol et
    @Transactional(readOnly = true)
    public boolean isStockAvailable(String sku, Integer requestedQuantity) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductSku(sku);
        if (inventoryOpt.isEmpty()) {
            return false;
        }

        Inventory inventory = inventoryOpt.get();
        return inventory.getQuantity() >= requestedQuantity;
    }

    //Mevcut stok miktarını getir
    @Transactional(readOnly = true)
    public Integer getAvailableStock(String sku) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByProductSku(sku);
        if (inventoryOpt.isEmpty()) {
            return 0;
        }

        return inventoryOpt.get().getQuantity();
    }
}