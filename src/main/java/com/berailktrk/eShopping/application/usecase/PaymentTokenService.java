package com.berailktrk.eShopping.application.usecase;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.berailktrk.eShopping.domain.model.PaymentToken;
import com.berailktrk.eShopping.domain.model.User;

/**
 * PaymentToken entity'si için uygulama servis katmanı
 * Ödeme token yönetimi ve karmaşık business logic'i yönetir
 */
@Service
public class PaymentTokenService {

    /**
     * Token'ın süresi dolmuş mu kontrol eder
     * 
     * @param paymentToken kontrol edilecek token
     * @return süre dolmuşsa true
     */
    public boolean isExpired(PaymentToken paymentToken) {
        if (paymentToken.getExpiryMonth() == null || paymentToken.getExpiryYear() == null) {
            return false; // Expiry bilgisi yoksa expired sayılmaz
        }

        YearMonth tokenExpiry = YearMonth.of(
            paymentToken.getExpiryYear(), 
            paymentToken.getExpiryMonth()
        );
        
        YearMonth currentMonth = YearMonth.now();
        
        // Token son kullanma ayının sonuna kadar geçerli
        return tokenExpiry.isBefore(currentMonth);
    }

    /**
     * Token'ın aktif ve kullanılabilir durumda olup olmadığını kontrol eder
     * 
     * @param paymentToken kontrol edilecek token
     * @return kullanılabilirse true
     */
    public boolean isValid(PaymentToken paymentToken) {
        return !isExpired(paymentToken);
    }

    /**
     * Token'ın varsayılan kart olup olmadığını kontrol eder
     * 
     * @param paymentToken kontrol edilecek token
     * @return varsayılan kartsa true
     */
    public boolean isDefault(PaymentToken paymentToken) {
        return Boolean.TRUE.equals(paymentToken.getIsDefault());
    }

    /**
     * Token'ı varsayılan kart olarak ayarlar
     * Not: Diğer kartların is_default'unu false yapmak repository seviyesinde yapılmalı
     * 
     * @param paymentToken varsayılan yapılacak token
     * @param allUserTokens kullanıcının tüm token'ları
     */
    public void setAsDefault(PaymentToken paymentToken, List<PaymentToken> allUserTokens) {
        // Önce tüm kartların default'unu kaldır
        for (PaymentToken token : allUserTokens) {
            if (!token.getId().equals(paymentToken.getId())) {
                token.setIsDefault(false);
            }
        }
        
        // Seçilen kartı default yap
        paymentToken.setIsDefault(true);
    }

    /**
     * Token'ı varsayılan karttan çıkarır
     * 
     * @param paymentToken güncellenecek token
     */
    public void removeAsDefault(PaymentToken paymentToken) {
        paymentToken.setIsDefault(false);
    }

    /**
     * Token'ın ne kadar süre sonra sona ereceğini hesaplar (ay cinsinden)
     * 
     * @param paymentToken kontrol edilecek token
     * @return kalan ay sayısı (negatif: süresi dolmuş)
     */
    public long getMonthsUntilExpiry(PaymentToken paymentToken) {
        if (paymentToken.getExpiryMonth() == null || paymentToken.getExpiryYear() == null) {
            return Long.MAX_VALUE; // Expiry bilgisi yoksa sınırsız
        }

        YearMonth tokenExpiry = YearMonth.of(
            paymentToken.getExpiryYear(), 
            paymentToken.getExpiryMonth()
        );
        
        YearMonth currentMonth = YearMonth.now();
        
        return currentMonth.until(tokenExpiry, java.time.temporal.ChronoUnit.MONTHS);
    }

    /**
     * Token'ın yakında (3 ay içinde) sona ereceğini kontrol eder
     * 
     * @param paymentToken kontrol edilecek token
     * @return 3 ay içinde sona erecekse true
     */
    public boolean isExpiringSoon(PaymentToken paymentToken) {
        long monthsUntilExpiry = getMonthsUntilExpiry(paymentToken);
        return monthsUntilExpiry >= 0 && monthsUntilExpiry <= 3;
    }

    /**
     * Expiry bilgisini günceller
     * 
     * @param paymentToken güncellenecek token
     * @param month yeni ay (1-12)
     * @param year yeni yıl (>= 2000)
     * @throws IllegalArgumentException geçersiz değerler girilirse
     */
    public void updateExpiry(PaymentToken paymentToken, int month, int year) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Ay değeri 1-12 arasında olmalıdır");
        }
        
        if (year < 2000) {
            throw new IllegalArgumentException("Yıl değeri 2000'den büyük olmalıdır");
        }

        paymentToken.setExpiryMonth(month);
        paymentToken.setExpiryYear(year);
    }

    /**
     * Last4 (son 4 hane) günceller
     * 
     * @param paymentToken güncellenecek token
     * @param last4 kartın son 4 hanesi
     * @throws IllegalArgumentException geçersiz format girilirse
     */
    public void updateLast4(PaymentToken paymentToken, String last4) {
        if (last4 == null || last4.length() != 4) {
            throw new IllegalArgumentException("Last4 değeri 4 karakter olmalıdır");
        }
        
        if (!last4.matches("\\d{4}")) {
            throw new IllegalArgumentException("Last4 sadece rakamlardan oluşmalıdır");
        }

        paymentToken.setLast4(last4);
    }

    /**
     * Provider token'ı günceller
     * 
     * @param paymentToken güncellenecek token
     * @param providerToken yeni provider token
     * @throws IllegalArgumentException token boşsa
     */
    public void updateProviderToken(PaymentToken paymentToken, String providerToken) {
        if (providerToken == null || providerToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Provider token boş olamaz");
        }

        paymentToken.setProviderToken(providerToken);
    }

    /**
     * Metadata günceller
     * 
     * @param paymentToken güncellenecek token
     * @param metadata yeni metadata
     */
    public void updateMetadata(PaymentToken paymentToken, Map<String, Object> metadata) {
        paymentToken.setMetadata(metadata);
    }

    /**
     * Metadata'ya yeni bir alan ekler veya günceller
     * 
     * @param paymentToken güncellenecek token
     * @param key metadata anahtarı
     * @param value metadata değeri
     */
    public void addOrUpdateMetadataField(PaymentToken paymentToken, String key, Object value) {
        Map<String, Object> metadata = paymentToken.getMetadata();
        if (metadata == null) {
            metadata = new java.util.HashMap<>();
            paymentToken.setMetadata(metadata);
        }
        metadata.put(key, value);
    }

    /**
     * Kart türü bilgisini metadata'ya ekler
     * 
     * @param paymentToken güncellenecek token
     * @param cardType kart türü (visa, mastercard, amex, vb.)
     */
    public void setCardType(PaymentToken paymentToken, String cardType) {
        addOrUpdateMetadataField(paymentToken, "card_type", cardType);
    }

    /**
     * Kart sahibinin adını metadata'ya ekler
     * 
     * @param paymentToken güncellenecek token
     * @param cardHolderName kart sahibi adı
     */
    public void setCardHolderName(PaymentToken paymentToken, String cardHolderName) {
        addOrUpdateMetadataField(paymentToken, "card_holder_name", cardHolderName);
    }

    /**
     * Token doğrulama
     * 
     * @param paymentToken doğrulanacak token
     * @throws IllegalStateException token geçersizse
     */
    public void validateToken(PaymentToken paymentToken) {
        if (paymentToken.getUser() == null) {
            throw new IllegalStateException("Payment token bir kullanıcıya ait olmalıdır");
        }

        if (paymentToken.getProvider() == null || paymentToken.getProvider().trim().isEmpty()) {
            throw new IllegalStateException("Provider bilgisi boş olamaz");
        }

        if (paymentToken.getProviderToken() == null || paymentToken.getProviderToken().trim().isEmpty()) {
            throw new IllegalStateException("Provider token boş olamaz");
        }

        // Expiry month kontrolü
        if (paymentToken.getExpiryMonth() != null) {
            if (paymentToken.getExpiryMonth() < 1 || paymentToken.getExpiryMonth() > 12) {
                throw new IllegalStateException("Expiry month 1-12 arasında olmalıdır");
            }
        }

        // Expiry year kontrolü
        if (paymentToken.getExpiryYear() != null) {
            if (paymentToken.getExpiryYear() < 2000) {
                throw new IllegalStateException("Expiry year 2000'den büyük olmalıdır");
            }
        }

        // Last4 kontrolü
        if (paymentToken.getLast4() != null) {
            if (paymentToken.getLast4().length() != 4) {
                throw new IllegalStateException("Last4 değeri 4 karakter olmalıdır");
            }
            if (!paymentToken.getLast4().matches("\\d{4}")) {
                throw new IllegalStateException("Last4 sadece rakamlardan oluşmalıdır");
            }
        }

        // Expiry kontrolü - hem ay hem de yıl olmalı
        if ((paymentToken.getExpiryMonth() != null && paymentToken.getExpiryYear() == null) ||
            (paymentToken.getExpiryMonth() == null && paymentToken.getExpiryYear() != null)) {
            throw new IllegalStateException("Expiry month ve year birlikte girilmelidir");
        }
    }

    /**
     * Token'ın ne kadar süredir kullanıldığını hesaplar (ay cinsinden)
     * 
     * @param paymentToken kontrol edilecek token
     * @return token yaşı (ay)
     */
    public long getTokenAgeInMonths(PaymentToken paymentToken) {
        java.time.Duration duration = java.time.Duration.between(
            paymentToken.getCreatedAt(), 
            Instant.now()
        );
        return duration.toDays() / 30; // Yaklaşık ay hesabı
    }

    /**
     * Provider'a göre token formatını doğrular
     * 
     * @param paymentToken kontrol edilecek token
     * @return geçerli formatsa true
     */
    public boolean hasValidProviderFormat(PaymentToken paymentToken) {
        String provider = paymentToken.getProvider().toLowerCase();
        String token = paymentToken.getProviderToken();

        // Provider'a özel format kontrolü
        switch (provider) {
            case "stripe":
                // Stripe token formatı: tok_* veya pm_*
                return token.startsWith("tok_") || token.startsWith("pm_") || token.startsWith("card_");
            
            case "adyen":
                // Adyen token formatı genelde UUID benzeri
                return token.length() > 10; // Basit kontrol
            
            case "paypal":
                // PayPal token formatı
                return token.startsWith("BA-") || token.length() > 10;
            
            default:
                // Bilinmeyen provider için temel kontrol
                return token.length() > 5;
        }
    }

    /**
     * Maskelenmiş kart numarası oluşturur (görüntüleme için)
     * 
     * @param paymentToken token
     * @return maskelenmiş kart numarası (örn: "**** **** **** 1234")
     */
    public String getMaskedCardNumber(PaymentToken paymentToken) {
        if (paymentToken.getLast4() == null) {
            return "**** **** **** ****";
        }
        return "**** **** **** " + paymentToken.getLast4();
    }

    /**
     * Kart görüntüleme metni oluşturur
     * 
     * @param paymentToken token
     * @return kart bilgisi (örn: "Visa ending in 1234 (expires 12/2025)")
     */
    public String getCardDisplayText(PaymentToken paymentToken) {
        StringBuilder display = new StringBuilder();
        
        // Kart türü (metadata'dan)
        if (paymentToken.getMetadata() != null && 
            paymentToken.getMetadata().containsKey("card_type")) {
            display.append(paymentToken.getMetadata().get("card_type"));
        } else {
            display.append("Card");
        }
        
        // Son 4 hane
        if (paymentToken.getLast4() != null) {
            display.append(" ending in ").append(paymentToken.getLast4());
        }
        
        // Son kullanma tarihi
        if (paymentToken.getExpiryMonth() != null && paymentToken.getExpiryYear() != null) {
            display.append(" (expires ")
                   .append(String.format("%02d", paymentToken.getExpiryMonth()))
                   .append("/")
                   .append(paymentToken.getExpiryYear())
                   .append(")");
        }
        
        // Varsayılan kart işareti
        if (isDefault(paymentToken)) {
            display.append(" [Default]");
        }
        
        // Süresi dolmuş uyarısı
        if (isExpired(paymentToken)) {
            display.append(" [EXPIRED]");
        } else if (isExpiringSoon(paymentToken)) {
            display.append(" [Expiring Soon]");
        }
        
        return display.toString();
    }

    /**
     * Kullanıcının varsayılan kartını bulur
     * 
     * @param userTokens kullanıcının tüm token'ları
     * @return varsayılan token (yoksa null)
     */
    public PaymentToken findDefaultToken(List<PaymentToken> userTokens) {
        return userTokens.stream()
            .filter(this::isDefault)
            .findFirst()
            .orElse(null);
    }

    /**
     * Kullanıcının geçerli (süresi dolmamış) kartlarını filtreler
     * 
     * @param userTokens kullanıcının tüm token'ları
     * @return geçerli token'lar
     */
    public List<PaymentToken> filterValidTokens(List<PaymentToken> userTokens) {
        return userTokens.stream()
            .filter(this::isValid)
            .toList();
    }

    /**
     * Kullanıcının süresi dolmuş kartlarını filtreler
     * 
     * @param userTokens kullanıcının tüm token'ları
     * @return süresi dolmuş token'lar
     */
    public List<PaymentToken> filterExpiredTokens(List<PaymentToken> userTokens) {
        return userTokens.stream()
            .filter(this::isExpired)
            .toList();
    }

    /**
     * Token'ı yeni bir kullanıcıya atar (opsiyonel - genelde yapılmaz)
     * 
     * @param paymentToken güncellenecek token
     * @param newUser yeni kullanıcı
     */
    public void transferToUser(PaymentToken paymentToken, User newUser) {
        if (newUser == null) {
            throw new IllegalArgumentException("Yeni kullanıcı null olamaz");
        }
        
        // Transfer ediliyorsa default olmaktan çıkar
        paymentToken.setIsDefault(false);
        paymentToken.setUser(newUser);
    }
}
