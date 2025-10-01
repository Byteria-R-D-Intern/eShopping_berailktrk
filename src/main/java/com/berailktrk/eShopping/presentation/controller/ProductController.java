package com.berailktrk.eShopping.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.berailktrk.eShopping.application.usecase.ProductService;
import com.berailktrk.eShopping.presentation.dto.request.CreateProductRequest;
import com.berailktrk.eShopping.presentation.dto.request.UpdateProductRequest;
import com.berailktrk.eShopping.presentation.dto.response.ProductResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Product Controller - Ürün yönetimi (PUBLIC: Listeleme, ADMIN: CRUD)
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Products", description = "Ürün yönetimi API'leri")
public class ProductController {

    private final ProductService productService;

    // Tüm aktif ürünleri listele - PUBLIC endpoint (Authentication gerekmez)
    @GetMapping
    @Operation(summary = "Tüm aktif ürünleri listele", description = "Kullanıcılar için aktif ürünleri getirir")
    public ResponseEntity<List<ProductResponse>> getAllActiveProducts() {
        log.info("GET /api/products - Fetching all active products");
        List<ProductResponse> products = productService.getAllActiveProducts();
        return ResponseEntity.ok(products);
    }

    // ID'ye göre ürün getir - PUBLIC endpoint
    @GetMapping("/{productId}")
    @Operation(summary = "ID'ye göre ürün getir", description = "Belirli bir ürünün detaylarını getirir")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID productId) {
        log.info("GET /api/products/{} - Fetching product", productId);
        ProductResponse product = productService.getProductById(productId);
        return ResponseEntity.ok(product);
    }

    // SKU'ya göre ürün getir - PUBLIC endpoint
    @GetMapping("/sku/{sku}")
    @Operation(summary = "SKU'ya göre ürün getir", description = "SKU ile ürün detaylarını getirir")
    public ResponseEntity<ProductResponse> getProductBySku(@PathVariable String sku) {
        log.info("GET /api/products/sku/{} - Fetching product", sku);
        ProductResponse product = productService.getProductBySku(sku);
        return ResponseEntity.ok(product);
    }

    // İsme göre ürün ara - PUBLIC endpoint (case-insensitive)
    @GetMapping("/search")
    @Operation(summary = "İsme göre ürün ara", description = "Ürün ismine göre arama yapar (case-insensitive)")
    public ResponseEntity<List<ProductResponse>> searchProducts(@RequestParam String name) {
        log.info("GET /api/products/search?name={} - Searching products", name);
        List<ProductResponse> products = productService.searchProductsByName(name);
        return ResponseEntity.ok(products);
    }

    // ========================================
    // ADMIN ENDPOINTS
    // ========================================

    // Tüm ürünleri listele - Aktif ve pasif tüm ürünler (ADMIN ONLY)
    @GetMapping("/admin/all")
    @Operation(
        summary = "Tüm ürünleri listele (Admin)", 
        description = "Aktif ve pasif tüm ürünleri getirir",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductResponse>> getAllProductsAdmin() {
        log.info("GET /api/products/admin/all - Fetching all products (admin)");
        List<ProductResponse> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    // Yeni ürün oluştur - ADMIN ONLY
    @PostMapping("/admin")
    @Operation(
        summary = "Yeni ürün oluştur (Admin)", 
        description = "Yeni ürün ekler ve opsiyonel olarak stok oluşturur",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        log.info("POST /api/products/admin - Creating product: {}", request.getSku());
        ProductResponse product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    // Ürün güncelle - ADMIN ONLY
    @PutMapping("/admin/{productId}")
    @Operation(
        summary = "Ürün güncelle (Admin)", 
        description = "Mevcut ürünü günceller",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateProductRequest request) {
        log.info("PUT /api/products/admin/{} - Updating product", productId);
        ProductResponse product = productService.updateProduct(productId, request);
        return ResponseEntity.ok(product);
    }

    // Ürün sil - Soft delete (ADMIN ONLY)
    @DeleteMapping("/admin/{productId}")
    @Operation(
        summary = "Ürün sil (Admin)", 
        description = "Ürünü pasif hale getirir (soft delete)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID productId) {
        log.info("DELETE /api/products/admin/{} - Deleting product", productId);
        productService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }
}
