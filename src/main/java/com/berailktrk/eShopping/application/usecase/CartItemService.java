package com.berailktrk.eShopping.application.usecase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.berailktrk.eShopping.domain.model.Cart;
import com.berailktrk.eShopping.domain.model.CartItem;
import com.berailktrk.eShopping.domain.model.Product;

/**
 * CartItem entity'si için uygulama servis katmanı
 * Sepet ürün yönetimi ve karmaşık business logic'i yönetir
 */
@Service
public class CartItemService {

    private static final int MAX_QUANTITY_PER_ITEM = 99; // Tek üründen maksimum miktar

    /**
     * Sepet itemının toplam fiyatını hesaplar
     * 
     * @param cartItem hesaplanacak sepet item'ı
     * @return toplam fiyat (qty * unit_price_snapshot)
     */
    public BigDecimal calculateItemTotal(CartItem cartItem) {
        return cartItem.getUnitPriceSnapshot()
            .multiply(BigDecimal.valueOf(cartItem.getQty()));
    }

    /**
     * Birden fazla cart item'ın toplam fiyatını hesaplar
     * 
     * @param cartItems hesaplanacak sepet item'ları
     * @return toplam fiyat
     */
    public BigDecimal calculateCartTotal(List<CartItem> cartItems) {
        return cartItems.stream()
            .map(this::calculateItemTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Sepetteki toplam ürün sayısını hesaplar (miktar bazında)
     * 
     * @param cartItems sepetteki item'lar
     * @return toplam ürün sayısı
     */
    public int calculateTotalItemCount(List<CartItem> cartItems) {
        return cartItems.stream()
            .mapToInt(CartItem::getQty)
            .sum();
    }

    /**
     * Sepetteki farklı ürün çeşit sayısını hesaplar
     * 
     * @param cartItems sepetteki item'lar
     * @return farklı ürün sayısı
     */
    public int calculateUniqueProductCount(List<CartItem> cartItems) {
        return cartItems.size();
    }

    /**
     * Cart item miktarını artırır
     * 
     * @param cartItem güncellenecek item
     * @param quantityToAdd eklenecek miktar
     * @throws IllegalArgumentException miktar geçersizse veya limit aşılırsa
     */
    public void increaseQuantity(CartItem cartItem, int quantityToAdd) {
        if (quantityToAdd <= 0) {
            throw new IllegalArgumentException("Eklenecek miktar pozitif olmalıdır");
        }

        int newQuantity = cartItem.getQty() + quantityToAdd;

        if (newQuantity > MAX_QUANTITY_PER_ITEM) {
            throw new IllegalArgumentException(
                String.format("Maksimum ürün miktarı (%d) aşılamaz", MAX_QUANTITY_PER_ITEM)
            );
        }

        cartItem.setQty(newQuantity);
    }

    /**
     * Cart item miktarını azaltır
     * 
     * @param cartItem güncellenecek item
     * @param quantityToRemove çıkarılacak miktar
     * @throws IllegalArgumentException miktar geçersizse veya sonuç sıfırdan küçükse
     */
    public void decreaseQuantity(CartItem cartItem, int quantityToRemove) {
        if (quantityToRemove <= 0) {
            throw new IllegalArgumentException("Çıkarılacak miktar pozitif olmalıdır");
        }

        int newQuantity = cartItem.getQty() - quantityToRemove;

        if (newQuantity <= 0) {
            throw new IllegalArgumentException(
                "Miktar sıfır veya altına düşemez. Item'ı silmek için ayrı method kullanın"
            );
        }

        cartItem.setQty(newQuantity);
    }

    /**
     * Cart item miktarını belirli bir değere günceller
     * 
     * @param cartItem güncellenecek item
     * @param newQuantity yeni miktar
     * @throws IllegalArgumentException miktar geçersizse
     */
    public void updateQuantity(CartItem cartItem, int newQuantity) {
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("Miktar pozitif olmalıdır");
        }

        if (newQuantity > MAX_QUANTITY_PER_ITEM) {
            throw new IllegalArgumentException(
                String.format("Maksimum ürün miktarı (%d) aşılamaz", MAX_QUANTITY_PER_ITEM)
            );
        }

        cartItem.setQty(newQuantity);
    }

    /**
     * Ürünün güncel fiyatı ile snapshot fiyatını karşılaştırır
     * 
     * @param cartItem kontrol edilecek item
     * @param currentProductPrice ürünün güncel fiyatı
     * @return fiyat değiştiyse true
     */
    public boolean hasPriceChanged(CartItem cartItem, BigDecimal currentProductPrice) {
        return cartItem.getUnitPriceSnapshot().compareTo(currentProductPrice) != 0;
    }

    /**
     * Fiyat snapshot'ını günceller (örn: fiyat değiştiğinde kullanıcıya bildirilir ve kabul ederse)
     * 
     * @param cartItem güncellenecek item
     * @param newPrice yeni snapshot fiyatı
     * @throws IllegalArgumentException fiyat geçersizse
     */
    public void updatePriceSnapshot(CartItem cartItem, BigDecimal newPrice) {
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Fiyat geçersiz");
        }

        cartItem.setUnitPriceSnapshot(newPrice);
    }

    /**
     * Cart item metadata'sını günceller
     * 
     * @param cartItem güncellenecek item
     * @param metadata yeni metadata
     */
    public void updateMetadata(CartItem cartItem, Map<String, Object> metadata) {
        cartItem.setMetadata(metadata);
    }

    /**
     * Cart item metadata'sına yeni bir alan ekler veya günceller
     * 
     * @param cartItem güncellenecek item
     * @param key metadata anahtarı
     * @param value metadata değeri
     */
    public void addOrUpdateMetadataField(CartItem cartItem, String key, Object value) {
        Map<String, Object> metadata = cartItem.getMetadata();
        if (metadata == null) {
            metadata = new java.util.HashMap<>();
            cartItem.setMetadata(metadata);
        }
        metadata.put(key, value);
    }

    /**
     * Cart item'ın geçerli olup olmadığını doğrular
     * 
     * @param cartItem doğrulanacak item
     * @throws IllegalStateException item geçersizse
     */
    public void validateCartItem(CartItem cartItem) {
        if (cartItem.getCart() == null) {
            throw new IllegalStateException("Cart item bir sepete ait olmalıdır");
        }

        if (cartItem.getProduct() == null) {
            throw new IllegalStateException("Cart item bir ürüne ait olmalıdır");
        }

        if (cartItem.getQty() <= 0) {
            throw new IllegalStateException("Miktar pozitif olmalıdır");
        }

        if (cartItem.getUnitPriceSnapshot() == null || 
            cartItem.getUnitPriceSnapshot().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Fiyat geçerli olmalıdır");
        }
    }

    /**
     * Fiyat farkını hesaplar (güncel fiyat - snapshot fiyat)
     * 
     * @param cartItem kontrol edilecek item
     * @param currentProductPrice ürünün güncel fiyatı
     * @return fiyat farkı (pozitif: zamlandı, negatif: indirimde)
     */
    public BigDecimal calculatePriceDifference(CartItem cartItem, BigDecimal currentProductPrice) {
        return currentProductPrice.subtract(cartItem.getUnitPriceSnapshot());
    }

    /**
     * Fiyat değişikliği yüzdesini hesaplar
     * 
     * @param cartItem kontrol edilecek item
     * @param currentProductPrice ürünün güncel fiyatı
     * @return yüzde değişim (örn: 10.5 = %10.5 artış, -5.0 = %5 indirim)
     */
    public BigDecimal calculatePriceChangePercentage(CartItem cartItem, BigDecimal currentProductPrice) {
        if (cartItem.getUnitPriceSnapshot().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal difference = calculatePriceDifference(cartItem, currentProductPrice);
        return difference
            .divide(cartItem.getUnitPriceSnapshot(), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Sepetteki item'ın ne kadar süredir bekletildiğini hesaplar (saat cinsinden)
     * 
     * @param cartItem kontrol edilecek item
     * @return sepete eklendiğinden beri geçen saat
     */
    public long getHoursSinceAdded(CartItem cartItem) {
        java.time.Duration duration = java.time.Duration.between(
            cartItem.getAddedAt(), 
            Instant.now()
        );
        return duration.toHours();
    }

    /**
     * Stokta olmayan veya fiyatı değişen item'ları filtreler
     * 
     * @param cartItems kontrol edilecek item'lar
     * @param inventoryService stok kontrolü için
     * @param productService ürün kontrolü için
     * @return sorunlu item'lar
     */
    public List<CartItem> findProblematicItems(
        List<CartItem> cartItems,
        InventoryService inventoryService,
        ProductService productService
    ) {
        return cartItems.stream()
            .filter(item -> {
                Product product = item.getProduct();
                
                // Ürün aktif değilse veya fiyat değiştiyse
                return !product.getIsActive() || 
                       hasPriceChanged(item, product.getPrice());
            })
            .toList();
    }

    /**
     * İki sepeti birleştirir (misafir + kullanıcı sepeti)
     * Not: Bu method sadece logic'i yönetir, kayıt repository'de yapılmalı
     * 
     * @param sourceItems kaynak sepet item'ları
     * @param targetCart hedef sepet
     * @return birleştirilmiş item'lar (yeni veya güncellenmiş)
     */
    public void mergeCartItems(List<CartItem> sourceItems, Cart targetCart, List<CartItem> targetItems) {
        for (CartItem sourceItem : sourceItems) {
            // Hedef sepette aynı ürün var mı?
            CartItem existingItem = targetItems.stream()
                .filter(item -> item.getProduct().getId().equals(sourceItem.getProduct().getId()))
                .findFirst()
                .orElse(null);

            if (existingItem != null) {
                // Aynı ürün varsa miktarı artır
                try {
                    increaseQuantity(existingItem, sourceItem.getQty());
                } catch (IllegalArgumentException e) {
                    // Limit aşılırsa maksimum değere ayarla
                    existingItem.setQty(MAX_QUANTITY_PER_ITEM);
                }
            } else {
                // Yeni ürünse hedef sepete ekle
                sourceItem.setCart(targetCart);
                targetItems.add(sourceItem);
            }
        }
    }
}
