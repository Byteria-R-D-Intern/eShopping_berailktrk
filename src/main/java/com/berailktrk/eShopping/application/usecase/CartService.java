package com.berailktrk.eShopping.application.usecase;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.berailktrk.eShopping.domain.model.Cart;
import com.berailktrk.eShopping.domain.model.User;

/**
 * Cart entity'si için uygulama servis katmanı
 * Sepet yönetimi ve karmaşık business logic'i yönetir
 */
@Service
public class CartService {

    private static final int CART_EXPIRATION_DAYS = 30; // Sepet 30 gün sonra pasif sayılır
    private static final int GUEST_CART_EXPIRATION_DAYS = 7; // Misafir sepeti 7 gün sonra pasif sayılır

    /**
     * Sepetin bir kullanıcıya ait olup olmadığını kontrol eder
     * 
     * @param cart kontrol edilecek sepet
     * @return kullanıcıya aitse true, misafir sepetiyse false
     */
    public boolean isUserCart(Cart cart) {
        return cart.getUser() != null;
    }

    /**
     * Sepetin misafir sepeti olup olmadığını kontrol eder
     * 
     * @param cart kontrol edilecek sepet
     * @return misafir sepetiyse true, değilse false
     */
    public boolean isGuestCart(Cart cart) {
        return cart.getUser() == null && cart.getSessionToken() != null;
    }

    /**
     * Sepetin süresi dolmuş mu kontrol eder
     * 
     * @param cart kontrol edilecek sepet
     * @return süre dolmuşsa true, değilse false
     */
    public boolean isExpired(Cart cart) {
        int expirationDays = isGuestCart(cart) ? GUEST_CART_EXPIRATION_DAYS : CART_EXPIRATION_DAYS;
        Instant expirationTime = cart.getUpdatedAt().plus(Duration.ofDays(expirationDays));
        return Instant.now().isAfter(expirationTime);
    }

    /**
     * Sepetin aktif olup olmadığını kontrol eder
     * 
     * @param cart kontrol edilecek sepet
     * @return aktifse true, süresi dolmuşsa false
     */
    public boolean isActive(Cart cart) {
        return !isExpired(cart);
    }

    /**
     * Sepeti kullanıcıya bağlar (misafir kullanıcı giriş yaptığında)
     * 
     * @param cart güncelle edilecek sepet
     * @param user sepete bağlanacak kullanıcı
     * @throws IllegalArgumentException sepet zaten bir kullanıcıya bağlıysa
     */
    public void assignToUser(Cart cart, User user) {
        if (user == null) {
            throw new IllegalArgumentException("Kullanıcı null olamaz");
        }

        if (cart.getUser() != null && !cart.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Sepet zaten başka bir kullanıcıya ait");
        }

        cart.setUser(user);
        cart.setUpdatedAt(Instant.now());
    }

    /**
     * Sepet güncellendiğinde updated_at zamanını günceller
     * 
     * @param cart güncellenecek sepet
     */
    public void touchCart(Cart cart) {
        cart.setUpdatedAt(Instant.now());
    }

    /**
     * Sepet metadata'sını günceller
     * 
     * @param cart güncellenecek sepet
     * @param metadata yeni metadata
     */
    public void updateMetadata(Cart cart, Map<String, Object> metadata) {
        cart.setMetadata(metadata);
        cart.setUpdatedAt(Instant.now());
    }

    /**
     * Sepet metadata'sına yeni bir alan ekler veya günceller
     * 
     * @param cart güncellenecek sepet
     * @param key metadata anahtarı
     * @param value metadata değeri
     */
    public void addOrUpdateMetadataField(Cart cart, String key, Object value) {
        Map<String, Object> metadata = cart.getMetadata();
        if (metadata == null) {
            metadata = new java.util.HashMap<>();
            cart.setMetadata(metadata);
        }
        metadata.put(key, value);
        cart.setUpdatedAt(Instant.now());
    }

    /**
     * Session token oluşturur (misafir sepeti için)
     * 
     * @return benzersiz session token
     */
    public String generateSessionToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Sepeti kullanıcıdan ayırır (örn: logout sonrası)
     * 
     * @param cart güncellenecek sepet
     */
    public void detachFromUser(Cart cart) {
        cart.setUser(null);
        cart.setUpdatedAt(Instant.now());
    }

    /**
     * Session token'ı günceller
     * 
     * @param cart güncellenecek sepet
     * @param sessionToken yeni session token
     */
    public void updateSessionToken(Cart cart, String sessionToken) {
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Session token boş olamaz");
        }
        cart.setSessionToken(sessionToken);
        cart.setUpdatedAt(Instant.now());
    }

    /**
     * Sepeti temizleme için hazırlar (örn: sipariş tamamlandıktan sonra)
     * Not: Gerçek temizlik CartItem'lar üzerinden yapılır, bu method sadece metadata temizler
     * 
     * @param cart temizlenecek sepet
     */
    public void clearMetadata(Cart cart) {
        cart.setMetadata(null);
        cart.setUpdatedAt(Instant.now());
    }

    /**
     * Sepetin yaşını hesaplar (gün cinsinden)
     * 
     * @param cart kontrol edilecek sepet
     * @return sepet yaşı (gün)
     */
    public long getCartAgeInDays(Cart cart) {
        Duration duration = Duration.between(cart.getCreatedAt(), Instant.now());
        return duration.toDays();
    }

    /**
     * Sepetin son güncellemeden sonra ne kadar süre geçtiğini hesaplar (gün cinsinden)
     * 
     * @param cart kontrol edilecek sepet
     * @return son güncelleme sonrası geçen gün sayısı
     */
    public long getDaysSinceLastUpdate(Cart cart) {
        Duration duration = Duration.between(cart.getUpdatedAt(), Instant.now());
        return duration.toDays();
    }

    /**
     * Sepeti doğrular
     * 
     * @param cart doğrulanacak sepet
     * @throws IllegalStateException sepet geçersizse
     */
    public void validateCart(Cart cart) {
        if (cart.getUser() == null && cart.getSessionToken() == null) {
            throw new IllegalStateException("Sepet ne bir kullanıcıya ne de session token'a sahip");
        }

        if (isExpired(cart)) {
            throw new IllegalStateException("Sepetin süresi dolmuş");
        }
    }

    /**
     * İki sepeti birleştirme işlemi öncesi kontrol yapar
     * Not: Gerçek birleştirme CartItem seviyesinde yapılır
     * 
     * @param sourceCart kaynak sepet
     * @param targetCart hedef sepet
     * @throws IllegalArgumentException sepetler birleştirilemezse
     */
    public void validateCartMerge(Cart sourceCart, Cart targetCart) {
        if (sourceCart == null || targetCart == null) {
            throw new IllegalArgumentException("Kaynak ve hedef sepet null olamaz");
        }

        if (sourceCart.getId().equals(targetCart.getId())) {
            throw new IllegalArgumentException("Aynı sepet kendi içine birleştirilemez");
        }

        if (isExpired(sourceCart)) {
            throw new IllegalArgumentException("Kaynak sepetin süresi dolmuş");
        }

        if (isExpired(targetCart)) {
            throw new IllegalArgumentException("Hedef sepetin süresi dolmuş");
        }
    }
}
