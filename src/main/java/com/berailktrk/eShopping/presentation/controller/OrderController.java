package com.berailktrk.eShopping.presentation.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.berailktrk.eShopping.application.usecase.OrderService;
import com.berailktrk.eShopping.domain.model.Order;
import com.berailktrk.eShopping.domain.model.User;
import com.berailktrk.eShopping.domain.repository.OrderRepository;
import com.berailktrk.eShopping.presentation.dto.request.CheckoutRequest;
import com.berailktrk.eShopping.presentation.dto.response.OrderResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OrderController - Sipariş yönetimi için REST API endpoints
 * 
 * Bu controller:
 * - Siparişleri listeler ve detaylarını getirir
 * - Sipariş durumlarını günceller
 * - Payment sistemi ile entegre çalışır
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Order Management", description = "Sipariş yönetimi API'leri")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @PostMapping("/checkout")
    @Operation(summary = "Sepetten sipariş oluştur (Online Ödeme)", 
               description = "Sepet içeriğinden yeni sipariş oluşturur ve sepeti temizler. Sadece online ödeme yöntemleri kabul edilir (CREDIT_CARD/DEBIT_CARD/BANK_TRANSFER). Ödeme yöntemi sequenceNumber ile belirtilir.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Sipariş başarıyla oluşturuldu"),
        @ApiResponse(responseCode = "400", description = "Sepet boş, geçersiz veri, desteklenmeyen ödeme yöntemi veya geçersiz sequence number"),
        @ApiResponse(responseCode = "401", description = "Yetkilendirme gerekli"),
        @ApiResponse(responseCode = "409", description = "Yetersiz stok")
    })
    public ResponseEntity<OrderResponse> checkout(
            @Valid @RequestBody CheckoutRequest request,
            Authentication authentication) {
        
        log.info("Checkout isteği - User: {}", getCurrentUserId(authentication));
        
        try {
            User currentUser = (User) authentication.getPrincipal();
            
            log.info("Checkout başlıyor - User: {}", currentUser.getId());
            
            Order order = orderService.createOrderFromCart(
                    currentUser.getId(),
                    request.getShippingAddress(),
                    request.getBillingAddress(),
                    request.getSequenceNumber(),
                    request.getOrderNotes(),
                    request.getMetadata()
            );
            
            log.info("Order oluşturuldu - Order ID: {}", order.getId());
            
            OrderResponse response = mapToOrderResponse(order);
            
            log.info("Response hazırlandı");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("Checkout sırasında hata:", e);
            throw e; // GlobalExceptionHandler'a yönlendir
        }
    }

    @GetMapping
    @Operation(summary = "Siparişleri listele", description = "Kullanıcının siparişlerini getirir")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Siparişler başarıyla getirildi"),
        @ApiResponse(responseCode = "401", description = "Yetkilendirme gerekli")
    })
    public ResponseEntity<List<OrderResponse>> getOrders(Authentication authentication) {
        
        log.info("Siparişler listeleme isteği - User: {}", getCurrentUserId(authentication));
        
        User currentUser = (User) authentication.getPrincipal();
        
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
        
        List<OrderResponse> responseList = orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseList);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Sipariş detayı", description = "Belirli bir siparişin detaylarını getirir")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sipariş detayları başarıyla getirildi"),
        @ApiResponse(responseCode = "401", description = "Yetkilendirme gerekli"),
        @ApiResponse(responseCode = "404", description = "Sipariş bulunamadı")
    })
    public ResponseEntity<OrderResponse> getOrder(
            @Parameter(description = "Sipariş ID") @PathVariable UUID orderId,
            Authentication authentication) {
        
        log.info("Sipariş detay isteği - User: {}, Order: {}", getCurrentUserId(authentication), orderId);
        
        User currentUser = (User) authentication.getPrincipal();
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Sipariş bulunamadı: " + orderId));
        
        if (!order.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Bu sipariş bu kullanıcıya ait değil");
        }
        
        OrderResponse response = mapToOrderResponse(order);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Sipariş iptal et", description = "Mevcut siparişi iptal eder")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sipariş başarıyla iptal edildi"),
        @ApiResponse(responseCode = "401", description = "Yetkilendirme gerekli"),
        @ApiResponse(responseCode = "404", description = "Sipariş bulunamadı"),
        @ApiResponse(responseCode = "409", description = "Sipariş iptal edilemez durumda")
    })
    public ResponseEntity<OrderResponse> cancelOrder(
            @Parameter(description = "Sipariş ID") @PathVariable UUID orderId,
            Authentication authentication) {
        
        log.info("Sipariş iptal isteği - User: {}, Order: {}", getCurrentUserId(authentication), orderId);
        
        User currentUser = (User) authentication.getPrincipal();
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Sipariş bulunamadı: " + orderId));
        
        if (!order.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Bu sipariş bu kullanıcıya ait değil");
        }
        
        orderService.cancelOrder(order);
        order = orderRepository.save(order);
        
        OrderResponse response = mapToOrderResponse(order);
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{orderId}/ship")
    @Operation(summary = "Siparişi kargoya ver", description = "Ödenmiş siparişi kargoya verir (Admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sipariş başarıyla kargoya verildi"),
        @ApiResponse(responseCode = "401", description = "Yetkilendirme gerekli"),
        @ApiResponse(responseCode = "403", description = "Admin yetkisi gerekli"),
        @ApiResponse(responseCode = "404", description = "Sipariş bulunamadı"),
        @ApiResponse(responseCode = "409", description = "Sipariş kargoya verilemez durumda")
    })
    public ResponseEntity<OrderResponse> shipOrder(
            @Parameter(description = "Sipariş ID") @PathVariable UUID orderId,
            Authentication authentication) {
        
        log.info("Sipariş kargoya verme isteği - User: {}, Order: {}", getCurrentUserId(authentication), orderId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Sipariş bulunamadı: " + orderId));
        
        orderService.markAsShipped(order);
        order = orderRepository.save(order);
        
        OrderResponse response = mapToOrderResponse(order);
        
        return ResponseEntity.ok(response);
    }

    // ==================== HELPER METHODS ====================

    private UUID getCurrentUserId(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return user.getId();
    }

    private OrderResponse mapToOrderResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .status(order.getStatus() != null ? order.getStatus().toString() : null)
                .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().toString() : null)
                .createdAt(order.getCreatedAt())
                .paidAt(order.getPaidAt())
                .cancelledAt(order.getCancelledAt())
                .shippingAddress(order.getShippingAddress())
                .billingAddress(order.getBillingAddress())
                .metadata(order.getMetadata())
                .build();
    }
}
