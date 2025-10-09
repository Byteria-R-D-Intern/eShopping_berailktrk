package com.berailktrk.eShopping.presentation.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.berailktrk.eShopping.application.usecase.CartService;
import com.berailktrk.eShopping.domain.model.CartItem;
import com.berailktrk.eShopping.presentation.dto.request.AddToCartRequest;
import com.berailktrk.eShopping.presentation.dto.request.UpdateCartItemRequest;
import com.berailktrk.eShopping.presentation.dto.response.CartItemResponse;
import com.berailktrk.eShopping.presentation.dto.response.CartResponse;
import com.berailktrk.eShopping.presentation.dto.response.CartClearResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Cart REST Controller - Sepet yönetimi endpoint'leri
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cart Management", description = "Sepet yönetimi endpoint'leri")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;

    //Sepete ürün ekle    
    @Operation(summary = "Sepete ürün ekle", 
               description = "Kullanıcının sepetine ürün ekler. Stok kontrolü yapılır ve rezervasyon oluşturulur.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Ürün başarıyla sepete eklendi"),
        @ApiResponse(responseCode = "400", description = "Validation hatası veya yetersiz stok"),
        @ApiResponse(responseCode = "401", description = "Kimlik doğrulama gerekli")
    })
    @PostMapping("/add")
    public ResponseEntity<CartItemResponse> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            Authentication authentication) {
        
        UUID userId = getCurrentUserId(authentication);
        log.info("Adding product to cart - User: {}, SKU: {}, Quantity: {}", userId, request.getProductSku(), request.getQuantity());
        
        CartItem cartItem = cartService.addToCart(userId, request.getProductSku(), request.getQuantity());
        java.math.BigDecimal totalPrice = cartService.calculateItemTotalPrice(cartItem);
        CartItemResponse response = mapToCartItemResponse(cartItem, totalPrice);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //Kullanıcının sepetini getir
    @Operation(summary = "Sepeti getir", 
               description = "Kullanıcının sepetindeki tüm ürünleri ve toplam bilgilerini döner.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sepet başarıyla getirildi"),
        @ApiResponse(responseCode = "401", description = "Kimlik doğrulama gerekli")
    })
    @GetMapping
    public ResponseEntity<CartResponse> getCart(Authentication authentication) {
        UUID userId = getCurrentUserId(authentication);
        log.info("Getting cart for user: {}", userId);
        
        List<CartItem> cartItems = cartService.getCartItems(userId);
        List<CartItemResponse> itemResponses = cartItems.stream()
                .map(item -> mapToCartItemResponse(item, cartService.calculateItemTotalPrice(item)))
                .collect(Collectors.toList());
        
        // Business logic hesaplamaları service'den al
        Integer totalItemCount = cartService.calculateTotalItemCount(cartItems);
        Integer uniqueItemCount = cartService.calculateUniqueItemCount(cartItems);
        java.math.BigDecimal totalAmount = cartService.calculateTotalAmount(cartItems);
        
        CartResponse response = CartResponse.builder()
                .userId(userId)
                .items(itemResponses)
                .totalItemCount(totalItemCount)
                .uniqueItemCount(uniqueItemCount)
                .totalAmount(totalAmount)
                .lastUpdate(cartItems.isEmpty() ? null : cartItems.get(0).getCart().getUpdatedAt())
                .build();
        
        return ResponseEntity.ok(response);
    }

    //Sepet kalemi miktarını güncelle
    @Operation(summary = "Sepet kalemi güncelle", 
               description = "Sepetteki bir ürünün miktarını günceller.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sepet kalemi başarıyla güncellendi"),
        @ApiResponse(responseCode = "400", description = "Validation hatası veya yetersiz stok"),
        @ApiResponse(responseCode = "401", description = "Kimlik doğrulama gerekli")
    })
    @PutMapping("/update")
    public ResponseEntity<CartItemResponse> updateCartItem(
            @Valid @RequestBody UpdateCartItemRequest request,
            Authentication authentication) {
        
        UUID userId = getCurrentUserId(authentication);
        log.info("Updating cart item - User: {}, SKU: {}, Quantity: {}", userId, request.getProductSku(), request.getQuantity());
        
        CartItem cartItem = cartService.updateCartItemQuantity(userId, request.getProductSku(), request.getQuantity());
        java.math.BigDecimal totalPrice = cartService.calculateItemTotalPrice(cartItem);
        CartItemResponse response = mapToCartItemResponse(cartItem, totalPrice);
        
        return ResponseEntity.ok(response);
    }

    //Sepetten ürün çıkar
    @Operation(summary = "Sepetten ürün çıkar", 
               description = "Sepetten belirtilen ürünü çıkarır. Quantity belirtilmezse tamamını çıkarır.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ürün başarıyla sepetten çıkarıldı"),
        @ApiResponse(responseCode = "400", description = "Ürün sepette bulunamadı"),
        @ApiResponse(responseCode = "401", description = "Kimlik doğrulama gerekli")
    })
    @DeleteMapping("/remove/{productSku}")
    public ResponseEntity<Void> removeFromCart(
            @Parameter(description = "Ürün SKU") @PathVariable String productSku,
            @Parameter(description = "Çıkarılacak miktar (opsiyonel)") @RequestParam(required = false) Integer quantity,
            Authentication authentication) {
        
        UUID userId = getCurrentUserId(authentication);
        log.info("Removing product from cart - User: {}, SKU: {}, Quantity: {}", userId, productSku, quantity);
        
        boolean success = cartService.removeFromCart(userId, productSku, quantity);
        if (success) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    //Sepeti tamamen temizle
    @Operation(summary = "Sepeti temizle", 
               description = "Kullanıcının sepetindeki tüm ürünleri çıkarır ve rezervasyonları iptal eder.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sepet başarıyla temizlendi"),
        @ApiResponse(responseCode = "401", description = "Kimlik doğrulama gerekli")
    })
    @DeleteMapping("/clear")
    public ResponseEntity<CartClearResponse> clearCart(Authentication authentication) {
        UUID userId = getCurrentUserId(authentication);
        log.info("Clearing cart for user: {}", userId);
        
        Integer removedItemCount = cartService.clearCart(userId);
        
        CartClearResponse response = CartClearResponse.builder()
                .userId(userId)
                .message("Sepet başarıyla temizlendi")
                .removedItemCount(removedItemCount)
                .clearedAt(java.time.Instant.now())
                .success(true)
                .build();
        
        return ResponseEntity.ok(response);
    }

    //Sepet toplam tutarını getir
    @Operation(summary = "Sepet toplam tutarını getir", 
               description = "Kullanıcının sepetinin toplam tutarını döner.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Toplam tutar başarıyla getirildi"),
        @ApiResponse(responseCode = "401", description = "Kimlik doğrulama gerekli")
    })
    @GetMapping("/total")
    public ResponseEntity<java.math.BigDecimal> getCartTotal(Authentication authentication) {
        UUID userId = getCurrentUserId(authentication);
        log.info("Getting cart total for user: {}", userId);
        
        java.math.BigDecimal total = cartService.getCartTotal(userId);
        return ResponseEntity.ok(total);
    }

    //Sepetteki ürün sayısını getir
    @Operation(summary = "Sepet ürün sayısını getir", 
               description = "Kullanıcının sepetindeki toplam ürün sayısını döner.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ürün sayısı başarıyla getirildi"),
        @ApiResponse(responseCode = "401", description = "Kimlik doğrulama gerekli")
    })
    @GetMapping("/count")
    public ResponseEntity<Integer> getCartItemCount(Authentication authentication) {
        UUID userId = getCurrentUserId(authentication);
        log.info("Getting cart item count for user: {}", userId);
        
        Integer count = cartService.getCartItemCount(userId);
        return ResponseEntity.ok(count);
    }

    //Authentication'dan kullanıcı ID'sini al
    private UUID getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("User not authenticated");
        }
        
        // Principal User objesi ise ID'sini al
        if (authentication.getPrincipal() instanceof com.berailktrk.eShopping.domain.model.User user) {
            return user.getId();
        }
        
        // Fallback: getName() email döndürür, UUID değil
        throw new IllegalArgumentException("Invalid authentication principal");
    }

    //CartItem entity'sini response DTO'ya dönüştür (totalPrice hesaplanmış olarak)
    private CartItemResponse mapToCartItemResponse(CartItem cartItem, java.math.BigDecimal totalPrice) {
        return CartItemResponse.builder()
                .id(cartItem.getId())
                .productSku(cartItem.getProduct().getSku())
                .productName(cartItem.getProduct().getName())
                .quantity(cartItem.getQty())
                .unitPrice(cartItem.getUnitPriceSnapshot())
                .totalPrice(totalPrice)
                .addedAt(cartItem.getAddedAt())
                .build();
    }
}
