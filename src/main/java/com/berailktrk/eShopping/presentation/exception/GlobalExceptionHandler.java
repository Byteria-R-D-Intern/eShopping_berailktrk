package com.berailktrk.eShopping.presentation.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

// Global Exception Handler - Tüm hatalar için merkezi yönetim (text/plain formatında)
// Sadece uygulama controller'larını kapsar (Swagger hariç)
@RestControllerAdvice(basePackages = "com.berailktrk.eShopping.presentation.controller")
@Slf4j
public class GlobalExceptionHandler {

    // Validation hataları - @Valid ile tetiklenen hatalar (400 Bad Request)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationErrors(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        
        StringBuilder message = new StringBuilder("Validation Error - Geçersiz veri:\n\n");
        
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            message.append("• ")
                   .append(error.getField())
                   .append(": ")
                   .append(error.getDefaultMessage())
                   .append("\n");
        }
        
        log.warn("Validation error: {}", message.toString().trim());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(message.toString().trim());
    }

    // IllegalArgument hataları - Geçersiz parametreler (400 Bad Request)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(
            IllegalArgumentException ex,
            WebRequest request) {
        
        String message = String.format(
            "400 Bad Request - Geçersiz İstek\n\n" +
            "Hata: %s\n\n" +
            "Lütfen gönderdiğiniz verileri kontrol edin.",
            ex.getMessage()
        );
        
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(message);
    }

    // IllegalState hataları - Business logic çakışmaları (409 Conflict)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(
            IllegalStateException ex,
            WebRequest request) {
        
        String message = String.format(
            "409 Conflict - İşlem Gerçekleştirilemedi\n\n" +
            "Hata: %s\n\n" +
            "Bu işlem şu anki durum ile uyuşmuyor.",
            ex.getMessage()
        );
        
        log.warn("Illegal state: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(message);
    }

    // Entity bulunamadı hataları (404 Not Found)
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleEntityNotFound(
            EntityNotFoundException ex,
            WebRequest request) {
        
        String message = String.format(
            "404 Not Found - Kayıt Bulunamadı\n\n" +
            "Hata: %s\n\n" +
            "İstenen kayıt sistemde bulunamadı.",
            ex.getMessage()
        );
        
        log.warn("Entity not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(message);
    }

    // Kimlik doğrulama hataları - Bad credentials, invalid token (401 Unauthorized)
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<String> handleAuthenticationException(
            Exception ex,
            WebRequest request) {
        
        String message = String.format(
            "401 Unauthorized - Kimlik Doğrulama Hatası\n\n" +
            "Hata: %s\n\n" +
            "Lütfen geçerli kullanıcı bilgileri ile giriş yapın.",
            ex.getMessage()
        );
        
        log.warn("Authentication error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(message);
    }

    // Yetkilendirme hataları - Access denied, rol/yetki eksikliği (403 Forbidden)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDenied(
            AccessDeniedException ex,
            WebRequest request) {
        
        String message = String.format(
            "403 Forbidden - Erişim Engellendi\n\n" +
            "Bu işlem için yetkiniz bulunmamaktadır.\n" +
            "Sadece yetkili kullanıcılar bu işlemi gerçekleştirebilir.\n\n" +
            "Endpoint: %s",
            request.getDescription(false).replace("uri=", "")
        );
        
        log.warn("Access denied: {}", request.getDescription(false));
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(message);
    }

    // Genel RuntimeException hataları (500 Internal Server Error)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(
            RuntimeException ex,
            WebRequest request) {
        
        String message = String.format(
            "500 Internal Server Error - Beklenmeyen Hata\n\n" +
            "Hata: %s\n\n" +
            "Bir sorun oluştu. Lütfen daha sonra tekrar deneyin.\n" +
            "Sorun devam ederse sistem yöneticisi ile iletişime geçin.",
            ex.getMessage() != null ? ex.getMessage() : "Bilinmeyen hata"
        );
        
        log.error("Runtime exception: ", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(message);
    }

    // Genel Exception yakalayıcı - Diğer handler'ların yakalamadığı tüm hatalar (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGlobalException(
            Exception ex,
            WebRequest request) {
        
        String message = String.format(
            "500 Internal Server Error - Sistem Hatası\n\n" +
            "Beklenmeyen bir hata oluştu.\n\n" +
            "Hata Detayı: %s\n\n" +
            "Lütfen sistem yöneticisi ile iletişime geçin.",
            ex.getClass().getSimpleName()
        );
        
        log.error("Unhandled exception: ", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(message);
    }
}

