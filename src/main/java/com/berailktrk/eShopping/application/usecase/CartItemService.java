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

//CartItem service - sepet ürün yönetimi ve business logic
@Service
public class CartItemService {

    private static final int MAX_QUANTITY_PER_ITEM = 99; // Tek üründen maksimum miktar

    //Sepet itemının toplam fiyatını hesapla (qty * unit_price_snapshot)
    public BigDecimal calculateItemTotal(CartItem cartItem) {
        return cartItem.getUnitPriceSnapshot()
            .multiply(BigDecimal.valueOf(cartItem.getQty()));
    }

    //Birden fazla cart item'ın toplam fiyatını hesapla
    public BigDecimal calculateCartTotal(List<CartItem> cartItems) {
        return cartItems.stream()
            .map(this::calculateItemTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    //Sepetteki toplam ürün sayısını hesapla (miktar bazında)
    public int calculateTotalItemCount(List<CartItem> cartItems) {
        return cartItems.stream()
            .mapToInt(CartItem::getQty)
            .sum();
    }

    //Sepetteki farklı ürün çeşit sayısını hesapla
    public int calculateUniqueProductCount(List<CartItem> cartItems) {
        return cartItems.size();
    }

    //Cart item miktarını artır
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

    //Cart item miktarını azalt
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

    //Cart item miktarını belirli bir değere güncelle
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

    //Ürünün güncel fiyatı ile snapshot fiyatını karşılaştır
    public boolean hasPriceChanged(CartItem cartItem, BigDecimal currentProductPrice) {
        return cartItem.getUnitPriceSnapshot().compareTo(currentProductPrice) != 0;
    }

    //Fiyat snapshot'ını güncelle (fiyat değiştiğinde kullanıcıya bildirilir ve kabul ederse)
    public void updatePriceSnapshot(CartItem cartItem, BigDecimal newPrice) {
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Fiyat geçersiz");
        }

        cartItem.setUnitPriceSnapshot(newPrice);
    }

    //Cart item metadata'sını güncelle
    public void updateMetadata(CartItem cartItem, Map<String, Object> metadata) {
        cartItem.setMetadata(metadata);
    }

    //Cart item metadata'sına yeni bir alan ekle veya güncelle
    public void addOrUpdateMetadataField(CartItem cartItem, String key, Object value) {
        Map<String, Object> metadata = cartItem.getMetadata();
        if (metadata == null) {
            metadata = new java.util.HashMap<>();
            cartItem.setMetadata(metadata);
        }
        metadata.put(key, value);
    }

    //Cart item'ın geçerli olup olmadığını doğrula
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

    //Fiyat farkını hesapla (güncel fiyat - snapshot fiyat)
    public BigDecimal calculatePriceDifference(CartItem cartItem, BigDecimal currentProductPrice) {
        return currentProductPrice.subtract(cartItem.getUnitPriceSnapshot());
    }

    //Fiyat değişikliği yüzdesini hesapla
    public BigDecimal calculatePriceChangePercentage(CartItem cartItem, BigDecimal currentProductPrice) {
        if (cartItem.getUnitPriceSnapshot().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal difference = calculatePriceDifference(cartItem, currentProductPrice);
        return difference
            .divide(cartItem.getUnitPriceSnapshot(), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }

    //Sepetteki item'ın ne kadar süredir bekletildiğini hesapla (saat cinsinden)
    public long getHoursSinceAdded(CartItem cartItem) {
        java.time.Duration duration = java.time.Duration.between(
            cartItem.getAddedAt(), 
            Instant.now()
        );
        return duration.toHours();
    }

    //Stokta olmayan veya fiyatı değişen item'ları filtrele
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

    //İki sepeti birleştir (misafir + kullanıcı sepeti)
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
