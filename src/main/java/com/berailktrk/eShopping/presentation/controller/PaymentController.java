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
import org.springframework.web.bind.annotation.RestController;

import com.berailktrk.eShopping.application.usecase.PaymentMethodService;
import com.berailktrk.eShopping.application.usecase.PaymentService;
import com.berailktrk.eShopping.domain.model.Payment;
import com.berailktrk.eShopping.domain.model.PaymentMethod;
import com.berailktrk.eShopping.domain.model.User;
import com.berailktrk.eShopping.presentation.dto.request.AddPaymentMethodRequest;
import com.berailktrk.eShopping.presentation.dto.request.InitiatePaymentRequest;
import com.berailktrk.eShopping.presentation.dto.request.RefundPaymentRequest;
import com.berailktrk.eShopping.presentation.dto.request.UpdatePaymentMethodRequest;
import com.berailktrk.eShopping.presentation.dto.response.CardInfoResponse;
import com.berailktrk.eShopping.presentation.dto.response.PaymentMethodListResponse;
import com.berailktrk.eShopping.presentation.dto.response.PaymentMethodResponse;
import com.berailktrk.eShopping.presentation.dto.response.PaymentListResponse;
import com.berailktrk.eShopping.presentation.dto.response.PaymentResponse;

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
 * PaymentController - Ödeme ve ödeme yöntemi yönetimi için REST API endpoints
 * 
 * Bu controller:
 * - Ödeme yöntemlerini yönetir (CRUD işlemleri)
 * - Ödeme işlemlerini yönetir (initiate, authorize, capture, refund)
 * - JWT authentication ile korunur
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Management", description = "Ödeme ve ödeme yöntemi yönetimi API'leri")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMethodService paymentMethodService;

    // ==================== PAYMENT METHOD ENDPOINTS ====================

    @PostMapping("/methods")
    @Operation(summary = "Yeni ödeme yöntemi ekle", description = "Kullanıcıya yeni ödeme yöntemi ekler")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Ödeme yöntemi başarıyla eklendi"),
        @ApiResponse(responseCode = "400", description = "Geçersiz istek verisi"),
        @ApiResponse(responseCode = "401", description = "Yetkilendirme gerekli"),
        @ApiResponse(responseCode = "409", description = "Aynı isimde ödeme yöntemi zaten mevcut")
    })
    public ResponseEntity<PaymentMethodResponse> addPaymentMethod(
            @Valid @RequestBody AddPaymentMethodRequest request,
            Authentication authentication) {
        
        log.info("Yeni ödeme yöntemi ekleme isteği - User: {}", getCurrentUserId(authentication));
        
        User currentUser = (User) authentication.getPrincipal();
        
        PaymentMethod paymentMethod = paymentMethodService.addPaymentMethod(
                currentUser.getId(),
                request.getMethodName(),
                request.getMethodType(),
                request.getCardNumber(),
                request.getCardholderName(),
                request.getExpiryDate(),
                request.getCvv(),
                request.getIsDefault()
        );
        
        PaymentMethodResponse response = mapToPaymentMethodResponse(paymentMethod);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/methods")
    @Operation(summary = "Ödeme yöntemlerini listele", description = "Kullanıcının ödeme yöntemlerini getirir")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ödeme yöntemleri başarıyla getirildi"),
        @ApiResponse(responseCode = "401", description = "Yetkilendirme gerekli")
    })
    public ResponseEntity<PaymentMethodListResponse> getPaymentMethods(Authentication authentication) {
        
        log.info("Ödeme yöntemleri listeleme isteği - User: {}", getCurrentUserId(authentication));
        
        User currentUser = (User) authentication.getPrincipal();
        
        List<PaymentMethod> paymentMethods = paymentMethodService.getPaymentMethodsByUserId(currentUser.getId());
        PaymentMethod defaultMethod = paymentMethodService.getDefaultPaymentMethod(currentUser.getId());
        
        List<PaymentMethodResponse> responseList = paymentMethods.stream()
                .map(this::mapToPaymentMethodResponse)
                .collect(Collectors.toList());
        
        PaymentMethodListResponse response = PaymentMethodListResponse.builder()
                .paymentMethods(responseList)
                .totalCount(responseList.size())
                .hasDefault(defaultMethod != null)
                .build();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/methods/{paymentMethodId}")
    @Operation(summary = "Ödeme yöntemi detayı", description = "Belirli bir ödeme yönteminin detaylarını getirir")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ödeme yöntemi detayları başarıyla getirildi"),
        @ApiResponse(responseCode = "401", description = "Yetkilendirme gerekli"),
        @ApiResponse(responseCode = "404", description = "Ödeme yöntemi bulunamadı")
    })
    public ResponseEntity<PaymentMethodResponse> getPaymentMethod(
            @Parameter(description = "Ödeme yöntemi ID") @PathVariable UUID paymentMethodId,
            Authentication authentication) {
        
        log.info("Ödeme yöntemi detay isteği - User: {}, Method: {}", getCurrentUserId(authentication), paymentMethodId);
        
        User currentUser = (User) authentication.getPrincipal();
        
        PaymentMethod paymentMethod = paymentMethodService.getPaymentMethodById(currentUser.getId(), paymentMethodId);
        PaymentMethodResponse response = mapToPaymentMethodResponse(paymentMethod);
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/methods/{paymentMethodId}")
    @Operation(summary = "Ödeme yöntemi güncelle", description = "Mevcut ödeme yöntemini günceller")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ödeme yöntemi başarıyla güncellendi"),
        @ApiResponse(responseCode = "400", description = "Geçersiz istek verisi"),
        @ApiResponse(responseCode = "401", description = "Yetkilendirme gerekli"),
        @ApiResponse(responseCode = "404", description = "Ödeme yöntemi bulunamadı"),
        @ApiResponse(responseCode = "409", description = "Aynı isimde ödeme yöntemi zaten mevcut")
    })
    public ResponseEntity<PaymentMethodResponse> updatePaymentMethod(
            @Parameter(description = "Ödeme yöntemi ID") @PathVariable UUID paymentMethodId,
            @Valid @RequestBody UpdatePaymentMethodRequest request,
            Authentication authentication) {
        
        log.info("Ödeme yöntemi güncelleme isteği - User: {}, Method: {}", getCurrentUserId(authentication), paymentMethodId);
        
        User currentUser = (User) authentication.getPrincipal();
        
        PaymentMethod paymentMethod = paymentMethodService.updatePaymentMethod(
                currentUser.getId(),
                paymentMethodId,
                request.getMethodName(),
                request.getIsDefault()
        );
        
        PaymentMethodResponse response = mapToPaymentMethodResponse(paymentMethod);
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/methods/{paymentMethodId}")
    @Operation(summary = "Ödeme yöntemi sil", description = "Mevcut ödeme yöntemini siler")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Ödeme yöntemi başarıyla silindi"),
        @ApiResponse(responseCode = "401", description = "Yetkilendirme gerekli"),
        @ApiResponse(responseCode = "404", description = "Ödeme yöntemi bulunamadı")
    })
    public ResponseEntity<Void> deletePaymentMethod(
            @Parameter(description = "Ödeme yöntemi ID") @PathVariable UUID paymentMethodId,
            Authentication authentication) {
        
        log.info("Ödeme yöntemi silme isteği - User: {}, Method: {}", getCurrentUserId(authentication), paymentMethodId);
        
        User currentUser = (User) authentication.getPrincipal();
        
        paymentMethodService.deletePaymentMethod(currentUser.getId(), paymentMethodId);
        
        return ResponseEntity.noContent().build();
    }

    // ==================== PAYMENT ENDPOINTS ====================

    @PostMapping("/initiate")
    @Operation(summary = "Ödeme işlemi başlat", description = "Yeni ödeme işlemi başlatır")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Ödeme işlemi başarıyla başlatıldı"),
        @ApiResponse(responseCode = "400", description = "Geçersiz istek verisi"),
        @ApiResponse(responseCode = "401", description = "Yetkilendirme gerekli"),
        @ApiResponse(responseCode = "404", description = "Sipariş veya ödeme yöntemi bulunamadı"),
        @ApiResponse(responseCode = "409", description = "Bu sipariş için zaten aktif ödeme var")
    })
    public ResponseEntity<PaymentResponse> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest request,
            Authentication authentication) {
        
        log.info("Ödeme işlemi başlatma isteği - User: {}, Order: {}", getCurrentUserId(authentication), request.getOrderId());
        
        User currentUser = (User) authentication.getPrincipal();
        
        Payment payment = paymentService.initiatePayment(
                request.getOrderId(),
                currentUser.getId(),
                request.getPaymentMethodId()
        );
        
        PaymentResponse response = mapToPaymentResponse(payment);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{paymentId}/authorize")
    @Operation(summary = "Ödeme yetkilendir", description = "Ödeme işlemini yetkilendirir")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ödeme başarıyla yetkilendirildi"),
        @ApiResponse(responseCode = "401", description = "Yetkilendirme gerekli"),
        @ApiResponse(responseCode = "404", description = "Ödeme bulunamadı"),
        @ApiResponse(responseCode = "409", description = "Ödeme yetkilendirilemez durumda")
    })
    public ResponseEntity<PaymentResponse> authorizePayment(
            @Parameter(description = "Payment ID") @PathVariable UUID paymentId,
            Authentication authentication) {
        
        log.info("Ödeme yetkilendirme isteği - User: {}, Payment: {}", getCurrentUserId(authentication), paymentId);
        
        Payment payment = paymentService.authorizePayment(paymentId);
        PaymentResponse response = mapToPaymentResponse(payment);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{paymentId}/capture")
    @Operation(summary = "Ödeme tahsil et", description = "Yetkilendirilmiş ödemeyi tahsil eder")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ödeme başarıyla tahsil edildi"),
        @ApiResponse(responseCode = "401", description = "Yetkilendirme gerekli"),
        @ApiResponse(responseCode = "404", description = "Ödeme bulunamadı"),
        @ApiResponse(responseCode = "409", description = "Ödeme tahsil edilemez durumda")
    })
    public ResponseEntity<PaymentResponse> capturePayment(
            @Parameter(description = "Payment ID") @PathVariable UUID paymentId,
            Authentication authentication) {
        
        log.info("Ödeme tahsil isteği - User: {}, Payment: {}", getCurrentUserId(authentication), paymentId);
        
        Payment payment = paymentService.capturePayment(paymentId);
        PaymentResponse response = mapToPaymentResponse(payment);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refund")
    @Operation(summary = "Ödeme iadesi", description = "Tahsil edilmiş ödemeyi iade eder")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ödeme başarıyla iade edildi"),
        @ApiResponse(responseCode = "400", description = "Geçersiz istek verisi"),
        @ApiResponse(responseCode = "401", description = "Yetkilendirme gerekli"),
        @ApiResponse(responseCode = "404", description = "Ödeme bulunamadı"),
        @ApiResponse(responseCode = "409", description = "Ödeme iade edilemez durumda")
    })
    public ResponseEntity<PaymentResponse> refundPayment(
            @Valid @RequestBody RefundPaymentRequest request,
            Authentication authentication) {
        
        log.info("Ödeme iadesi isteği - User: {}, Payment: {}", getCurrentUserId(authentication), request.getPaymentId());
        
        Payment payment = paymentService.refundPayment(request.getPaymentId(), request.getRefundAmount());
        PaymentResponse response = mapToPaymentResponse(payment);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Sipariş ödemelerini listele", description = "Belirli bir siparişe ait ödemeleri getirir")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ödemeler başarıyla getirildi"),
        @ApiResponse(responseCode = "401", description = "Yetkilendirme gerekli"),
        @ApiResponse(responseCode = "404", description = "Sipariş bulunamadı")
    })
    public ResponseEntity<PaymentListResponse> getPaymentsByOrderId(
            @Parameter(description = "Sipariş ID") @PathVariable UUID orderId,
            Authentication authentication) {
        
        log.info("Sipariş ödemeleri listeleme isteği - User: {}, Order: {}", getCurrentUserId(authentication), orderId);
        
        List<Payment> payments = paymentService.getPaymentsByOrderId(orderId);
        
        List<PaymentResponse> responseList = payments.stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
        
        PaymentListResponse response = PaymentListResponse.builder()
                .payments(responseList)
                .totalCount(responseList.size())
                .build();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    @Operation(summary = "Ödeme geçmişi", description = "Kullanıcının ödeme geçmişini getirir")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ödeme geçmişi başarıyla getirildi"),
        @ApiResponse(responseCode = "401", description = "Yetkilendirme gerekli")
    })
    public ResponseEntity<PaymentListResponse> getPaymentHistory(Authentication authentication) {
        
        log.info("Ödeme geçmişi isteği - User: {}", getCurrentUserId(authentication));
        
        User currentUser = (User) authentication.getPrincipal();
        
        List<Payment> payments = paymentService.getPaymentsByUserId(currentUser.getId());
        
        List<PaymentResponse> responseList = payments.stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
        
        PaymentListResponse response = PaymentListResponse.builder()
                .payments(responseList)
                .totalCount(responseList.size())
                .build();
        
        return ResponseEntity.ok(response);
    }

    // ==================== HELPER METHODS ====================

    private UUID getCurrentUserId(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return user.getId();
    }

    private PaymentMethodResponse mapToPaymentMethodResponse(PaymentMethod paymentMethod) {
        CardInfoResponse cardInfoResponse = null;
        if (paymentMethod.getCardInfo() != null) {
            cardInfoResponse = CardInfoResponse.builder()
                    .token(paymentMethod.getCardInfo().getToken())
                    .cardholderName(paymentMethod.getCardInfo().getCardholderName())
                    .expiryDate(paymentMethod.getCardInfo().getExpiryDate())
                    .maskedCardNumber(paymentMethod.getCardInfo().getMaskedCardNumber())
                    .cardType(paymentMethod.getCardInfo().getCardType())
                    .createdAt(paymentMethod.getCardInfo().getCreatedAt())
                    .expiresAt(paymentMethod.getCardInfo().getExpiresAt())
                    .build();
        }

        return PaymentMethodResponse.builder()
                .id(paymentMethod.getId())
                .methodName(paymentMethod.getMethodName())
                .methodType(paymentMethod.getMethodType())
                .cardInfo(cardInfoResponse)
                .isDefault(paymentMethod.getIsDefault())
                .isActive(paymentMethod.getIsActive())
                .createdAt(paymentMethod.getCreatedAt())
                .updatedAt(paymentMethod.getUpdatedAt())
                .build();
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder() != null ? payment.getOrder().getId() : null)
                .userId(payment.getUser() != null ? payment.getUser().getId() : null)
                .paymentMethodId(payment.getPaymentMethod() != null ? payment.getPaymentMethod().getId() : null)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus() != null ? payment.getStatus().toString() : null)
                .transactionId(payment.getTransactionId())
                .authorizationCode(payment.getAuthorizationCode())
                .responseCode(payment.getResponseCode())
                .responseMessage(payment.getResponseMessage())
                .createdAt(payment.getCreatedAt())
                .completedAt(payment.getCompletedAt())
                .failedAt(payment.getFailedAt())
                .refundedAt(payment.getRefundedAt())
                .refundAmount(payment.getRefundAmount())
                .build();
    }
}
