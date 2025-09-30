package com.berailktrk.eShopping.presentation.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.berailktrk.eShopping.presentation.dto.response.HealthResponse;
import com.berailktrk.eShopping.presentation.dto.response.WelcomeResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Welcome controller - Ana sayfa ve health check
 */
@RestController
@Tag(name = "Info", description = "API bilgilendirme endpoint'leri")
public class WelcomeController {

    @Operation(summary = "API Bilgisi", description = "API hakkında genel bilgi döner")
    @ApiResponse(responseCode = "200", description = "API bilgileri")
    @GetMapping("/")
    public WelcomeResponse welcome() {
        return WelcomeResponse.builder()
            .application("eShopping API")
            .version("1.0.0")
            .status("running")
            .message("Welcome to eShopping API")
            .endpoints(Map.of(
                "register", "POST /api/auth/register",
                "login", "POST /api/auth/login"
            ))
            .build();
    }

    @Operation(summary = "Health Check", description = "Uygulamanın sağlık durumunu kontrol eder")
    @ApiResponse(responseCode = "200", description = "Uygulama çalışıyor")
    @GetMapping("/health")
    public HealthResponse health() {
        return HealthResponse.builder()
            .status("UP")
            .message("eShopping API is healthy")
            .build();
    }
}
