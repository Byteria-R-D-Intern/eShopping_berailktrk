package com.berailktrk.eShopping.application.usecase;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.berailktrk.eShopping.domain.model.Inventory;
import com.berailktrk.eShopping.domain.model.Product;
import com.berailktrk.eShopping.domain.repository.InventoryRepository;
import com.berailktrk.eShopping.domain.repository.ProductRepository;
import com.berailktrk.eShopping.presentation.dto.request.CreateProductRequest;
import com.berailktrk.eShopping.presentation.dto.request.UpdateProductRequest;
import com.berailktrk.eShopping.presentation.dto.response.ProductResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.berailktrk.eShopping.application.mapper.ProductMapper;

// Product Service - Domain ve infrastructure arasındaki orchestration
// ADMIN: Tüm CRUD işlemleri, USER: Sadece listeleme ve görüntüleme
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductMapper productMapper;

    /**
     * Yeni ürün oluştur (ADMIN)
     * Opsiyonel olarak initial stok da eklenebilir
     */
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Creating new product with SKU: {}", request.getSku());

        // SKU kontrolü
        if (productRepository.existsBySku(request.getSku())) {
            throw new IllegalArgumentException("Product with SKU " + request.getSku() + " already exists");
        }

        // Ürün oluştur
        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .currency(request.getCurrency())
                .isActive(request.getIsActive())
                .metadata(request.getMetadata())
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created with ID: {}", savedProduct.getId());

        // Eğer initial stok verilmişse, inventory kaydı oluştur
        if (request.getInitialStockQuantity() != null && request.getInitialStockQuantity() > 0) {
            Inventory inventory = Inventory.builder()
                    .product(savedProduct)
                    .productId(savedProduct.getId())
                    .quantity(request.getInitialStockQuantity())
                    .reserved(0)
                    .build();
            inventoryRepository.save(inventory);
            log.info("Initial stock created for product: {} with quantity: {}", 
                     savedProduct.getSku(), request.getInitialStockQuantity());
        }

        return productMapper.toResponse(savedProduct);
    }

    /**
     * Ürün güncelle (ADMIN)
     * Sadece gönderilen alanlar güncellenir
     */
    @Transactional
    public ProductResponse updateProduct(UUID productId, UpdateProductRequest request) {
        log.info("Updating product with ID: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        // Sadece null olmayan alanları güncelle
        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getCurrency() != null) {
            product.setCurrency(request.getCurrency());
        }
        if (request.getIsActive() != null) {
            product.setIsActive(request.getIsActive());
        }
        if (request.getMetadata() != null) {
            product.setMetadata(request.getMetadata());
        }

        product.setUpdatedAt(Instant.now());
        Product updatedProduct = productRepository.save(product);
        
        log.info("Product updated: {}", updatedProduct.getSku());
        return productMapper.toResponse(updatedProduct);
    }

    /**
     * Ürün sil (ADMIN)
     * Soft delete - sadece isActive = false yapar
     */
    @Transactional
    public void deleteProduct(UUID productId) {
        log.info("Deleting (deactivating) product with ID: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        product.setIsActive(false);
        product.setUpdatedAt(Instant.now());
        productRepository.save(product);

        log.info("Product deactivated: {}", product.getSku());
    }

    /**
     * ID'ye göre ürün getir (USER + ADMIN)
     * Stok bilgisi ile birlikte
     */
    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID productId) {
        log.debug("Fetching product with ID: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        // Stok bilgisini getir ve DTO'ya dönüştür
        return inventoryRepository.findByProduct(product)
                .map(inventory -> productMapper.toResponseWithStock(
                        product, 
                        inventory.getQuantity(), 
                        inventory.getReserved()))
                .orElse(productMapper.toResponse(product));
    }

    /**
     * SKU'ya göre ürün getir (USER + ADMIN)
     */
    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {
        log.debug("Fetching product with SKU: {}", sku);

        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with SKU: " + sku));

        // Stok bilgisini getir ve DTO'ya dönüştür
        return inventoryRepository.findByProduct(product)
                .map(inventory -> productMapper.toResponseWithStock(
                        product, 
                        inventory.getQuantity(), 
                        inventory.getReserved()))
                .orElse(productMapper.toResponse(product));
    }

    /**
     * Tüm aktif ürünleri listele (USER)
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllActiveProducts() {
        log.debug("Fetching all active products");

        return productRepository.findByIsActiveTrue().stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Tüm ürünleri listele - aktif ve pasif (ADMIN)
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        log.debug("Fetching all products (including inactive)");

        return productRepository.findAll().stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * İsme göre ürün ara (USER + ADMIN)
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> searchProductsByName(String name) {
        log.debug("Searching products with name containing: {}", name);

        return productRepository.searchByName(name).stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }
}