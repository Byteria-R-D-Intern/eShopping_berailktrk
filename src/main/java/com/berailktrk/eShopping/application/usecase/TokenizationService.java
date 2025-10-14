package com.berailktrk.eShopping.application.usecase;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.berailktrk.eShopping.domain.model.CardInfo;

import lombok.extern.slf4j.Slf4j;


//TokenizationService - Kart bilgilerini token'a çevirir ve güvenli şekilde saklar

//Bu servis:
//- Kart bilgilerini alır ve benzersiz token üretir
//- Token-kart eşleştirmesini bellekte saklar
//- Token'ları expire edebilir
//- Gerçek kart bilgileri hiçbir zaman veritabanında saklanmaz

@Service
@Slf4j
public class TokenizationService {

    
    //Token-kart bilgileri eşleştirmesi
    //Key: Token, Value: CardInfo

    private final Map<String, CardInfo> tokenCardMap = new ConcurrentHashMap<>();

    //Token expire süresi (saat cinsinden)
    //Varsayılan: 24 saat
    
    @Value("${payment.token.expiration-hours:24}")
    private int tokenExpirationHours;

    //Kart bilgilerini token'a çevirir
    
    //@param cardNumber Gerçek kart numarası
    //@param cardholderName Kart sahibinin adı
    //@param expiryDate Son kullanma tarihi (MM/YY)
    //@param cvv CVV kodu
    //@return Tokenized CardInfo
    
    public CardInfo tokenizeCard(String cardNumber, String cardholderName, String expiryDate, String cvv) {
        log.info("Kart tokenization işlemi başlatıldı - Kart sahibi: {}", cardholderName);
        
        // Kart numarasını doğrula
        validateCardNumber(cardNumber);
        
        // Token üret
        String token = generateToken();
        
        // Maskelenmiş kart numarası oluştur
        String maskedCardNumber = maskCardNumber(cardNumber);
        
        // Kart türünü belirle
        String cardType = determineCardType(cardNumber);
        
        // Expire tarihini hesapla
        Instant expiresAt = Instant.now().plus(tokenExpirationHours, ChronoUnit.HOURS);
        
        // CardInfo oluştur
        CardInfo cardInfo = CardInfo.builder()
                .token(token)
                .cardholderName(cardholderName)
                .expiryDate(expiryDate)
                .maskedCardNumber(maskedCardNumber)
                .cardType(cardType)
                .createdAt(Instant.now())
                .expiresAt(expiresAt)
                .build();
        
        // Token-kart eşleştirmesini sakla
        tokenCardMap.put(token, cardInfo);
        
        log.info("Kart başarıyla tokenized edildi - Token: {}, Masked: {}", token, maskedCardNumber);
        
        return cardInfo;
    }

    //Token ile kart bilgilerini getirir
    
    //@param token Token
    //@return CardInfo veya null
    
    public CardInfo getCardInfoByToken(String token) {
        log.debug("Token ile kart bilgileri sorgulanıyor - Token: {}", token);
        
        CardInfo cardInfo = tokenCardMap.get(token);
        
        if (cardInfo == null) {
            log.warn("Token bulunamadı: {}", token);
            return null;
        }
        
        // Token expire kontrolü
        if (isTokenExpired(cardInfo)) {
            log.warn("Token süresi dolmuş: {}", token);
            tokenCardMap.remove(token);
            return null;
        }
        
        return cardInfo;
    }

    //Token'ı geçersiz kılar (silir)
    
    //@param token Token
    //@return Silme işlemi başarılı mı
    
    public boolean invalidateToken(String token) {
        log.info("Token geçersiz kılınıyor: {}", token);
        
        CardInfo removed = tokenCardMap.remove(token);
        boolean success = removed != null;
        
        if (success) {
            log.info("Token başarıyla geçersiz kılındı: {}", token);
        } else {
            log.warn("Token bulunamadı, geçersiz kılınamadı: {}", token);
        }
        
        return success;
    }

    //Süresi dolmuş token'ları temizler
    
    public void cleanupExpiredTokens() {
        log.info("Süresi dolmuş token'lar temizleniyor...");
        
        int initialSize = tokenCardMap.size();
        
        tokenCardMap.entrySet().removeIf(entry -> {
            boolean expired = isTokenExpired(entry.getValue());
            if (expired) {
                log.debug("Süresi dolmuş token siliniyor: {}", entry.getKey());
            }
            return expired;
        });
        
        int removedCount = initialSize - tokenCardMap.size();
        log.info("Süresi dolmuş {} token temizlendi. Kalan token sayısı: {}", removedCount, tokenCardMap.size());
    }

    
    //Aktif token sayısını döndürür
    
    //@return Aktif token sayısı
    
    public int getActiveTokenCount() {
        return tokenCardMap.size();
    }

    
    //Token üretir
    //Format: tok_[32 karakterlik rastgele string]
    
    //@return Benzersiz token
    
    private String generateToken() {
        String randomPart = UUID.randomUUID().toString().replace("-", "");
        return "tok_" + randomPart;
    }

    //Kart numarasını maskeler
    //Format: ****1234
    
    //@param cardNumber Gerçek kart numarası
    //@return Maskelenmiş kart numarası
    
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        
        String lastFour = cardNumber.substring(cardNumber.length() - 4);
        return "****" + lastFour;
    }

    //Kart türünü belirler
    
    //@param cardNumber Kart numarası
    //@return Kart türü
    
    private String determineCardType(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) {
            return "UNKNOWN";
        }
        
        // Visa: 4 ile başlar
        if (cardNumber.startsWith("4")) {
            return "VISA";
        }
        
        // Mastercard: 5 ile başlar veya 2 ile başlar (yeni format)
        if (cardNumber.startsWith("5") || cardNumber.startsWith("2")) {
            return "MASTERCARD";
        }
        
        return "UNKNOWN";
    }

    //Kart numarasını doğrular
    
    //@param cardNumber Kart numarası
    
    private void validateCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Kart numarası boş olamaz");
        }
        
        // Sadece rakam kontrolü
        String cleanNumber = cardNumber.replaceAll("\\s+", "");
        if (!cleanNumber.matches("\\d+")) {
            throw new IllegalArgumentException("Kart numarası sadece rakam içermelidir");
        }
        
        // Uzunluk kontrolü (13-19 karakter arası)
        if (cleanNumber.length() < 13 || cleanNumber.length() > 19) {
            throw new IllegalArgumentException("Kart numarası 13-19 karakter arası olmalıdır");
        }
        
        // Luhn algoritması kontrolü
        if (!isValidLuhn(cleanNumber)) {
            throw new IllegalArgumentException("Geçersiz kart numarası");
        }
    }

    //Luhn algoritması ile kart numarası doğrulaması
    
    //@param cardNumber Temizlenmiş kart numarası
    //@return Geçerli mi
    
    private boolean isValidLuhn(String cardNumber) {
        int sum = 0;
        boolean alternate = false;
        
        // Sağdan sola doğru işle
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }
            
            sum += digit;
            alternate = !alternate;
        }
        
        return (sum % 10) == 0;
    }

    //Token'ın süresi dolmuş mu kontrol eder
    
    //@param cardInfo CardInfo
    //@return Süresi dolmuş mu
    
    private boolean isTokenExpired(CardInfo cardInfo) {
        if (cardInfo.getExpiresAt() == null) {
            return false; // Expire tarihi yoksa süresiz
        }
        
        return Instant.now().isAfter(cardInfo.getExpiresAt());
    }
}
