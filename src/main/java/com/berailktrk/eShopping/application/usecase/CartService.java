package com.berailktrk.eShopping.application.usecase;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.berailktrk.eShopping.domain.model.AuditLog;
import com.berailktrk.eShopping.domain.model.Cart;
import com.berailktrk.eShopping.domain.model.CartItem;
import com.berailktrk.eShopping.domain.model.Product;
import com.berailktrk.eShopping.domain.model.User;
import com.berailktrk.eShopping.domain.repository.AuditLogRepository;
import com.berailktrk.eShopping.domain.repository.CartItemRepository;
import com.berailktrk.eShopping.domain.repository.CartRepository;
import com.berailktrk.eShopping.domain.repository.ProductRepository;
import com.berailktrk.eShopping.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//Cart business logic service
//Sepet yönetimi, ürün ekleme/çıkarma, stok kontrolü

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final AuditLogService auditLogService;
    private final AuditLogRepository auditLogRepository;

    //Kullanıcının sepetini getir veya oluştur
    
    public Cart getOrCreateCart(UUID userId) {
        log.info("Getting or creating cart for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Optional<Cart> existingCart = cartRepository.findByUserId(userId);
        if (existingCart.isPresent()) {
            Cart cart = existingCart.get();
            cartRepository.updateCartTimestamp(userId);
            return cart;
        }

        // Yeni sepet oluştur
        Cart newCart = Cart.builder()
                .user(user)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return cartRepository.save(newCart);
    }

    //Sepete ürün ekle
    
    public CartItem addToCart(UUID userId, String productSku, Integer quantity) {
        log.info("Adding product to cart - User: {}, SKU: {}, Quantity: {}", userId, productSku, quantity);
        
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        // Ürünü bul
        Product product = productRepository.findBySku(productSku)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with SKU: " + productSku));

        if (!product.getIsActive()) {
            throw new IllegalArgumentException("Product is not active: " + productSku);
        }

        // Stok kontrolü
        if (!inventoryService.isStockAvailable(productSku, quantity)) {
            throw new IllegalArgumentException("Insufficient stock for product: " + productSku);
        }

        // Sepeti getir veya oluştur
        Cart cart = getOrCreateCart(userId);

        // Mevcut cart item'ı kontrol et
        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId());
        
        if (existingItem.isPresent()) {
            // Mevcut item'ı güncelle
            CartItem cartItem = existingItem.get();
            Integer newQuantity = cartItem.getQty() + quantity;
            
            // Toplam stok kontrolü
            if (!inventoryService.isStockAvailable(productSku, newQuantity)) {
                throw new IllegalArgumentException("Insufficient stock for total quantity: " + newQuantity);
            }
            
            cartItem.setQty(newQuantity);
            CartItem updatedItem = cartItemRepository.save(cartItem);
            
            // Stok rezervasyonu yap
            boolean reserved = inventoryService.reserveStock(productSku, quantity, null);
            if (!reserved) {
                throw new IllegalStateException("Failed to reserve stock for product: " + productSku);
            }
            
            // Audit log kaydet
            Map<String, Object> details = new HashMap<>();
            details.put("productSku", productSku);
            details.put("quantity", quantity);
            details.put("oldQuantity", cartItem.getQty() - quantity);
            details.put("newQuantity", cartItem.getQty());
            details.put("action", "update_existing_item");
            
            AuditLog cartLog = auditLogService.createLogWithDetails(
                null, // Sistem işlemi
                "CART_ITEM_UPDATED",
                AuditLogService.RESOURCE_CART,
                cart.getId(),
                String.format("Sepete ürün eklendi: %s, Miktar: %d", productSku, quantity),
                details
            );
            auditLogRepository.save(cartLog);
            
            cartRepository.updateCartTimestamp(userId);
            return updatedItem;
        } else {
            // Yeni cart item oluştur
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .qty(quantity)
                    .unitPriceSnapshot(product.getPrice())
                    .addedAt(Instant.now())
                    .createdAt(Instant.now())
                    .build();

            CartItem savedItem = cartItemRepository.save(newItem);
            
            // Stok rezervasyonu yap
            boolean reserved = inventoryService.reserveStock(productSku, quantity, null);
            if (!reserved) {
                cartItemRepository.delete(savedItem);
                throw new IllegalStateException("Failed to reserve stock for product: " + productSku);
            }
            
            // Audit log kaydet
            Map<String, Object> details = new HashMap<>();
            details.put("productSku", productSku);
            details.put("quantity", quantity);
            details.put("unitPrice", product.getPrice());
            details.put("action", "add_new_item");
            
            AuditLog cartLog = auditLogService.createLogWithDetails(
                null, // Sistem işlemi
                "CART_ITEM_ADDED",
                AuditLogService.RESOURCE_CART,
                cart.getId(),
                String.format("Sepete yeni ürün eklendi: %s, Miktar: %d", productSku, quantity),
                details
            );
            auditLogRepository.save(cartLog);
            
            cartRepository.updateCartTimestamp(userId);
            return savedItem;
        }
    }

    //Kullanıcının sepetindeki tüm ürünleri getir
    
    @Transactional(readOnly = true)
    public List<CartItem> getCartItems(UUID userId) {
        log.info("Getting cart items for user: {}", userId);
        
        Optional<Cart> cart = cartRepository.findByUserId(userId);
        if (cart.isEmpty()) {
            return List.of(); // Boş liste döndür
        }

        return cartItemRepository.findByCartId(cart.get().getId());
    }

    
    //Sepet toplam tutarını hesapla
    
    @Transactional(readOnly = true)
    public BigDecimal getCartTotal(UUID userId) {
        log.info("Calculating cart total for user: {}", userId);
        
        List<CartItem> cartItems = getCartItems(userId);
        
        return cartItems.stream()
                .map(item -> item.getUnitPriceSnapshot().multiply(BigDecimal.valueOf(item.getQty())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    //Sepetten ürün çıkar
    public boolean removeFromCart(UUID userId, String productSku, Integer quantity) {
        log.info("Removing product from cart - User: {}, SKU: {}, Quantity: {}", userId, productSku, quantity);
        
        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findBySku(productSku)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with SKU: " + productSku));

        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId());
        if (existingItem.isEmpty()) {
            throw new IllegalArgumentException("Product not found in cart: " + productSku);
        }

        CartItem cartItem = existingItem.get();
        
        if (quantity == null || quantity >= cartItem.getQty()) {
            // Tamamını çıkar
            Integer removedQuantity = cartItem.getQty();
            cartItemRepository.deleteByCartIdAndProductId(cart.getId(), product.getId());
            inventoryService.cancelReservation(productSku, removedQuantity);
            
            // Audit log kaydet
            Map<String, Object> details = new HashMap<>();
            details.put("productSku", productSku);
            details.put("removedQuantity", removedQuantity);
            details.put("action", "remove_completely");
            
            AuditLog cartLog = auditLogService.createLogWithDetails(
                null, // Sistem işlemi
                "CART_ITEM_REMOVED",
                AuditLogService.RESOURCE_CART,
                cart.getId(),
                String.format("Sepetten ürün tamamen çıkarıldı: %s, Miktar: %d", productSku, removedQuantity),
                details
            );
            auditLogRepository.save(cartLog);
        } else {
            // Kısmi çıkarma
            Integer newQuantity = cartItem.getQty() - quantity;
            cartItem.setQty(newQuantity);
            cartItemRepository.save(cartItem);
            inventoryService.cancelReservation(productSku, quantity);
            
            // Audit log kaydet
            Map<String, Object> details = new HashMap<>();
            details.put("productSku", productSku);
            details.put("removedQuantity", quantity);
            details.put("oldQuantity", cartItem.getQty() + quantity);
            details.put("newQuantity", newQuantity);
            details.put("action", "remove_partial");
            
            AuditLog cartLog = auditLogService.createLogWithDetails(
                null, // Sistem işlemi
                "CART_ITEM_UPDATED",
                AuditLogService.RESOURCE_CART,
                cart.getId(),
                String.format("Sepetten ürün kısmen çıkarıldı: %s, Çıkarılan: %d", productSku, quantity),
                details
            );
            auditLogRepository.save(cartLog);
        }

        cartRepository.updateCartTimestamp(userId);
        return true;
    }

    //Sepet kalemi miktarını güncelle
    
    public CartItem updateCartItemQuantity(UUID userId, String productSku, Integer newQuantity) {
        log.info("Updating cart item quantity - User: {}, SKU: {}, New Quantity: {}", userId, productSku, newQuantity);
        
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findBySku(productSku)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with SKU: " + productSku));

        CartItem existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found in cart: " + productSku));

        // Stok kontrolü
        if (!inventoryService.isStockAvailable(productSku, newQuantity)) {
            throw new IllegalArgumentException("Insufficient stock for quantity: " + newQuantity);
        }

        Integer oldQuantity = existingItem.getQty();
        Integer quantityDifference = newQuantity - oldQuantity;

        // Miktar güncelle
        existingItem.setQty(newQuantity);
        CartItem updatedItem = cartItemRepository.save(existingItem);

        // Stok rezervasyonu güncelle
        if (quantityDifference > 0) {
            boolean reserved = inventoryService.reserveStock(productSku, quantityDifference, null);
            if (!reserved) {
                existingItem.setQty(oldQuantity);
                cartItemRepository.save(existingItem);
                throw new IllegalStateException("Failed to reserve additional stock for product: " + productSku);
            }
        } else if (quantityDifference < 0) {
            inventoryService.cancelReservation(productSku, Math.abs(quantityDifference));
        }

        cartRepository.updateCartTimestamp(userId);
        return updatedItem;
    }

    //Sepetteki toplam ürün sayısını getir
    
    @Transactional(readOnly = true)
    public Integer getCartItemCount(UUID userId) {
        log.info("Getting cart item count for user: {}", userId);
        
        Optional<Cart> cart = cartRepository.findByUserId(userId);
        if (cart.isEmpty()) {
            return 0;
        }

        Long count = cartItemRepository.getTotalQuantityByCartId(cart.get().getId());
        return count.intValue();
    }

    //Sepeti temizle
    
    public boolean clearCart(UUID userId) {
        log.info("Clearing cart for user: {}", userId);
        
        Cart cart = getOrCreateCart(userId);
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());

        // Tüm rezervasyonları iptal et
        for (CartItem item : cartItems) {
            inventoryService.cancelReservation(item.getProduct().getSku(), item.getQty());
        }

        // Tüm cart item'ları sil
        cartItemRepository.deleteByCartId(cart.getId());
        
        // Audit log kaydet
        Map<String, Object> details = new HashMap<>();
        details.put("clearedItemsCount", cartItems.size());
        details.put("totalQuantity", cartItems.stream().mapToInt(CartItem::getQty).sum());
        details.put("action", "clear_all");
        
        AuditLog cartLog = auditLogService.createLogWithDetails(
            null, // Sistem işlemi
            "CART_CLEARED",
            AuditLogService.RESOURCE_CART,
            cart.getId(),
            String.format("Sepet temizlendi: %d ürün, %d adet", 
                cartItems.size(), 
                cartItems.stream().mapToInt(CartItem::getQty).sum()),
            details
        );
        auditLogRepository.save(cartLog);
        
        cartRepository.updateCartTimestamp(userId);

        return true;
    }

    //Sepet kalemi için toplam fiyat hesapla (birim fiyat × miktar)
    public BigDecimal calculateItemTotalPrice(CartItem cartItem) {
        if (cartItem.getUnitPriceSnapshot() == null || cartItem.getQty() == null) {
            return BigDecimal.ZERO;
        }
        return cartItem.getUnitPriceSnapshot().multiply(BigDecimal.valueOf(cartItem.getQty()));
    }

    //Sepet için toplam ürün sayısını hesapla (tüm kalemlerin miktar toplamı)
    public Integer calculateTotalItemCount(List<CartItem> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            return 0;
        }
        return cartItems.stream()
                .mapToInt(CartItem::getQty)
                .sum();
    }

    //Sepet için farklı ürün sayısını hesapla (unique product sayısı)
    public Integer calculateUniqueItemCount(List<CartItem> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            return 0;
        }
        return cartItems.size();
    }

    //Sepet için toplam tutarı hesapla (tüm kalemlerin toplam fiyatı)
    public BigDecimal calculateTotalAmount(List<CartItem> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return cartItems.stream()
                .map(this::calculateItemTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}