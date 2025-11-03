package com.berailktrk.eShopping.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.berailktrk.eShopping.application.usecase.TokenizationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Payment System Configuration
 * 
 * Bu konfigürasyon:
 * - TokenizationService için scheduled task'ları yönetir
 * - Expired token'ları temizler
 * - Payment sistemi için genel ayarları içerir
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class PaymentConfig {

    private final TokenizationService tokenizationService;

    @Value("${payment.cleanup.expired-tokens-interval-hours:1}")
    private int cleanupIntervalHours;

    /**
     * Süresi dolmuş token'ları temizler
     * Varsayılan olarak her saat çalışır
     */
    @Scheduled(fixedRateString = "${payment.cleanup.expired-tokens-interval-hours:1}", 
               timeUnit = java.util.concurrent.TimeUnit.HOURS)
    public void cleanupExpiredTokens() {
        log.info("Scheduled token cleanup başlatılıyor...");
        
        try {
            int beforeCount = tokenizationService.getActiveTokenCount();
            tokenizationService.cleanupExpiredTokens();
            int afterCount = tokenizationService.getActiveTokenCount();
            
            log.info("Token cleanup tamamlandı. Temizlenen token sayısı: {}, Kalan token sayısı: {}", 
                    beforeCount - afterCount, afterCount);
        } catch (Exception e) {
            log.error("Token cleanup sırasında hata oluştu", e);
        }
    }
}
