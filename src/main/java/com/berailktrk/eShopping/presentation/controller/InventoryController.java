package com.berailktrk.eShopping.presentation.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.berailktrk.eShopping.application.usecase.InventoryService;
import com.berailktrk.eShopping.domain.model.Inventory;
import com.berailktrk.eShopping.presentation.dto.response.InventoryResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Inventory REST Controller - Stok yönetimi endpoint'leri (Admin only)
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Inventory Management", description = "Stok yönetimi endpoint'leri [ADMIN ONLY]")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    private final InventoryService inventoryService;

    // Düşük stoklu ürünleri getir - Belirtilen eşik değerin altındaki ürünler
    @Operation(summary = "Düşük stoklu ürünleri getir", 
               description = "Belirtilen eşik değerin altında stoklu ürünleri listeler. Admin yetkisi gereklidir.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Düşük stoklu ürünler başarıyla getirildi"),
        @ApiResponse(responseCode = "403", description = "Admin yetkisi gerekli")
    })
    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<InventoryResponse>> getLowStockItems(
            @Parameter(description = "Stok eşik değeri") @RequestParam(defaultValue = "10") Integer threshold) {
        log.info("Getting low stock items with threshold: {}", threshold);
        
        List<Inventory> inventories = inventoryService.getLowStockItems(threshold);
        List<InventoryResponse> responses = inventories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    // Stok tükenmiş ürünleri getir - Stok miktarı 0 olan ürünler
    @Operation(summary = "Stok tükenmiş ürünleri getir", 
               description = "Stok miktarı 0 olan ürünleri listeler. Admin yetkisi gereklidir.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stok tükenmiş ürünler başarıyla getirildi"),
        @ApiResponse(responseCode = "403", description = "Admin yetkisi gerekli")
    })
    @GetMapping("/out-of-stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<InventoryResponse>> getOutOfStockItems() {
        log.info("Getting out of stock items");
        
        List<Inventory> inventories = inventoryService.getOutOfStockItems();
        List<InventoryResponse> responses = inventories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    // ==================== SKU-BASED ENDPOINTS ====================

    // Stok kaydı oluştur - SKU ile yeni stok kaydı oluştur
    @Operation(summary = "Stok kaydı oluştur", 
               description = "SKU ile yeni stok kaydı oluşturur. Admin yetkisi gereklidir.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Stok kaydı başarıyla oluşturuldu"),
        @ApiResponse(responseCode = "400", description = "Validation hatası veya SKU bulunamadı"),
        @ApiResponse(responseCode = "409", description = "SKU için stok kaydı zaten mevcut"),
        @ApiResponse(responseCode = "403", description = "Admin yetkisi gerekli")
    })
    @PostMapping("/{sku}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryResponse> createInventory(
            @Parameter(description = "Ürün SKU") @PathVariable String sku,
            @Parameter(description = "Başlangıç stok miktarı") @RequestParam Integer initialQuantity) {
        log.info("Creating inventory for SKU: {} with quantity: {}", sku, initialQuantity);
        
        Inventory inventory = inventoryService.createInventory(sku, initialQuantity);
        InventoryResponse response = mapToResponse(inventory);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Stok miktarını artır/azalt - SKU ile stok miktarını delta değeri kadar değiştir
    @Operation(summary = "Stok miktarını artır/azalt", 
               description = "SKU ile ürünün stok miktarını artırır veya azaltır. Pozitif değer: artırma (ör: +10 depoya ürün geldi), Negatif değer: azaltma (ör: -2 bozuk ürün). Admin yetkisi gereklidir.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stok başarıyla güncellendi"),
        @ApiResponse(responseCode = "400", description = "Validation hatası, yetersiz stok veya SKU bulunamadı"),
        @ApiResponse(responseCode = "403", description = "Admin yetkisi gerekli")
    })
    @PutMapping("/{sku}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryResponse> adjustStock(
            @Parameter(description = "Ürün SKU") @PathVariable String sku,
            @Parameter(description = "Artırılacak/azaltılacak miktar (pozitif: +10, negatif: -2)") @RequestParam Integer delta) {
        log.info("Adjusting stock for SKU: {} by delta: {}", sku, delta);
        
        Inventory inventory = inventoryService.adjustStock(sku, delta);
        InventoryResponse response = mapToResponse(inventory);
        
        return ResponseEntity.ok(response);
    }

    // Ürün stok bilgilerini getir - SKU ile ürünün stok bilgilerini döndür
    @Operation(summary = "Ürün stok bilgilerini getir", 
               description = "SKU ile ürünün stok bilgilerini döner. Admin yetkisi gereklidir.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stok bilgileri başarıyla getirildi"),
        @ApiResponse(responseCode = "404", description = "SKU için stok kaydı bulunamadı"),
        @ApiResponse(responseCode = "403", description = "Admin yetkisi gerekli")
    })
    @GetMapping("/{sku}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryResponse> getInventory(
            @Parameter(description = "Ürün SKU") @PathVariable String sku) {
        log.info("Getting inventory for SKU: {}", sku);
        
        Inventory inventory = inventoryService.getInventoryBySku(sku);
        InventoryResponse response = mapToResponse(inventory);
        
        return ResponseEntity.ok(response);
    }

    // Stok durumunu kontrol et - Belirtilen miktar için stok uygunluğu kontrolü
    @Operation(summary = "Stok durumunu kontrol et", 
               description = "SKU ile ürünün belirtilen miktarda stokunun olup olmadığını kontrol eder. Admin yetkisi gereklidir.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stok durumu başarıyla kontrol edildi"),
        @ApiResponse(responseCode = "404", description = "SKU için stok kaydı bulunamadı"),
        @ApiResponse(responseCode = "403", description = "Admin yetkisi gerekli")
    })
    @GetMapping("/{sku}/availability")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Boolean> checkStockAvailability(
            @Parameter(description = "Ürün SKU") @PathVariable String sku,
            @Parameter(description = "İstenen miktar") @RequestParam Integer quantity) {
        log.info("Checking stock availability for SKU: {} quantity: {}", sku, quantity);
        
        boolean available = inventoryService.isStockAvailable(sku, quantity);
        return ResponseEntity.ok(available);
    }

    // Mevcut stok miktarını getir - SKU ile ürünün mevcut stok miktarı
    @Operation(summary = "Mevcut stok miktarını getir", 
               description = "SKU ile ürünün mevcut stok miktarını döner. Admin yetkisi gereklidir.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stok miktarı başarıyla getirildi"),
        @ApiResponse(responseCode = "404", description = "SKU için stok kaydı bulunamadı"),
        @ApiResponse(responseCode = "403", description = "Admin yetkisi gerekli")
    })
    @GetMapping("/{sku}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Integer> getAvailableStock(
            @Parameter(description = "Ürün SKU") @PathVariable String sku) {
        log.info("Getting available stock for SKU: {}", sku);
        
        Integer stock = inventoryService.getAvailableStock(sku);
        return ResponseEntity.ok(stock);
    }

    // Inventory entity'sini response DTO'ya dönüştür
    private InventoryResponse mapToResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .productId(inventory.getProduct().getId())
                .productName(inventory.getProduct().getName())
                .productSku(inventory.getProduct().getSku())
                .quantity(inventory.getQuantity())
                .reserved(inventory.getReserved())
                .updatedAt(inventory.getUpdatedAt())
                .version(inventory.getVersion())
                .build();
    }
}
