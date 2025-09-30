package com.berailktrk.eShopping.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.berailktrk.eShopping.application.usecase.AuthenticationUseCase;
import com.berailktrk.eShopping.presentation.dto.request.LoginRequest;
import com.berailktrk.eShopping.presentation.dto.request.RegisterRequest;
import com.berailktrk.eShopping.presentation.dto.response.AuthResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Authentication REST controller
 * Kullanıcı kayıt ve giriş endpoint'leri
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Kullanıcı kimlik doğrulama endpoint'leri")
public class AuthController {

    private final AuthenticationUseCase authenticationUseCase;

    /**
     * Authorization header'dan token'ı ayıklar
     */
    private String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return authorizationHeader != null ? authorizationHeader.trim() : null;
    }

    /**
     * Kullanıcı kaydı
     * 
     * POST /api/auth/register
     * 
     * @param request kayıt isteği
     * @return authentication yanıtı
     */
    @Operation(summary = "Kullanıcı kaydı", description = "Yeni bir kullanıcı kaydı oluşturur ve JWT token döner.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Kayıt başarılı, JWT token döndü"),
        @ApiResponse(responseCode = "400", description = "Validation hatası veya email zaten kullanımda"),
        @ApiResponse(responseCode = "500", description = "Sunucu hatası")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authenticationUseCase.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Kullanıcı girişi
     * 
     * POST /api/auth/login
     * 
     * @param request giriş isteği
     * @return authentication yanıtı
     */
    @Operation(summary = "Kullanıcı girişi", description = "Email ve şifre ile giriş yapar, JWT token döner.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Giriş başarılı, JWT token döndü"),
        @ApiResponse(responseCode = "401", description = "Email veya şifre hatalı"),
        @ApiResponse(responseCode = "403", description = "Hesap kilitli veya aktif değil"),
        @ApiResponse(responseCode = "500", description = "Sunucu hatası")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authenticationUseCase.login(request);
        return ResponseEntity.ok(response);
    }
}
