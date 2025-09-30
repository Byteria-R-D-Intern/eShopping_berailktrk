package com.berailktrk.eShopping.application.usecase;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.berailktrk.eShopping.domain.model.Cart;
import com.berailktrk.eShopping.domain.model.CartItem;
import com.berailktrk.eShopping.domain.model.Order;
import com.berailktrk.eShopping.domain.model.OrderItem;
import com.berailktrk.eShopping.domain.model.Product;

/**
 * OrderItem entity'si için uygulama servis katmanı
 * Sipariş ürün yönetimi ve karmaşık business logic'i yönetir
 */
@Service
public class OrderItemService {

    /**
     * Order item'ın toplam fiyatını hesaplar (qty × unit_price)
     * 
     * @param orderItem hesaplanacak order item
     * @return toplam fiyat
     */
    public BigDecimal calculateItemTotal(OrderItem orderItem) {
        return orderItem.getUnitPrice()
            .multiply(BigDecimal.valueOf(orderItem.getQty()));
    }

    /**
     * Birden fazla order item'ın toplam fiyatını hesaplar
     * 
     * @param orderItems hesaplanacak order item'lar
     * @return toplam fiyat
     */
    public BigDecimal calculateOrderTotal(List<OrderItem> orderItems) {
        return orderItems.stream()
            .map(OrderItem::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Siparişin toplam ürün sayısını hesaplar (miktar bazında)
     * 
     * @param orderItems siparişin item'ları
     * @return toplam ürün sayısı
     */
    public int calculateTotalItemCount(List<OrderItem> orderItems) {
        return orderItems.stream()
            .mapToInt(OrderItem::getQty)
            .sum();
    }

    /**
     * Siparişin farklı ürün çeşit sayısını hesaplar
     * 
     * @param orderItems siparişin item'ları
     * @return farklı ürün sayısı
     */
    public int calculateUniqueProductCount(List<OrderItem> orderItems) {
        return orderItems.size();
    }

    /**
     * Total price'ın doğru hesaplanıp hesaplanmadığını kontrol eder
     * Database constraint: total_price = unit_price * qty
     * 
     * @param orderItem kontrol edilecek item
     * @return doğru hesaplanmışsa true
     */
    public boolean isTotalPriceValid(OrderItem orderItem) {
        BigDecimal calculatedTotal = calculateItemTotal(orderItem);
        return orderItem.getTotalPrice().compareTo(calculatedTotal) == 0;
    }

    /**
     * Cart item'dan order item oluşturur
     * 
     * @param cartItem kaynak cart item
     * @param order hedef sipariş
     * @return yeni order item
     */
    public OrderItem createFromCartItem(CartItem cartItem, Order order) {
        BigDecimal totalPrice = cartItem.getUnitPriceSnapshot()
            .multiply(BigDecimal.valueOf(cartItem.getQty()));

        return OrderItem.builder()
            .order(order)
            .product(cartItem.getProduct())
            .unitPrice(cartItem.getUnitPriceSnapshot())
            .qty(cartItem.getQty())
            .totalPrice(totalPrice)
            .metadata(cartItem.getMetadata())
            .build();
    }

    /**
     * Sepetten sipariş item'ları oluşturur
     * 
     * @param cartItems sepet item'ları
     * @param order hedef sipariş
     * @return order item'lar listesi
     */
    public List<OrderItem> createOrderItemsFromCart(List<CartItem> cartItems, Order order) {
        return cartItems.stream()
            .map(cartItem -> createFromCartItem(cartItem, order))
            .toList();
    }

    /**
     * Metadata günceller
     * 
     * @param orderItem güncellenecek item
     * @param metadata yeni metadata
     */
    public void updateMetadata(OrderItem orderItem, Map<String, Object> metadata) {
        orderItem.setMetadata(metadata);
    }

    /**
     * Metadata'ya yeni bir alan ekler veya günceller
     * 
     * @param orderItem güncellenecek item
     * @param key metadata anahtarı
     * @param value metadata değeri
     */
    public void addOrUpdateMetadataField(OrderItem orderItem, String key, Object value) {
        Map<String, Object> metadata = orderItem.getMetadata();
        if (metadata == null) {
            metadata = new java.util.HashMap<>();
            orderItem.setMetadata(metadata);
        }
        metadata.put(key, value);
    }

    /**
     * Order item doğrulama
     * 
     * @param orderItem doğrulanacak item
     * @throws IllegalStateException item geçersizse
     */
    public void validateOrderItem(OrderItem orderItem) {
        if (orderItem.getOrder() == null) {
            throw new IllegalStateException("Order item bir siparişe ait olmalıdır");
        }

        if (orderItem.getProduct() == null) {
            throw new IllegalStateException("Order item bir ürüne ait olmalıdır");
        }

        if (orderItem.getQty() == null || orderItem.getQty() <= 0) {
            throw new IllegalStateException("Miktar pozitif olmalıdır");
        }

        if (orderItem.getUnitPrice() == null || 
            orderItem.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Birim fiyat geçerli olmalıdır");
        }

        if (orderItem.getTotalPrice() == null || 
            orderItem.getTotalPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Toplam fiyat geçerli olmalıdır");
        }

        // Total price doğruluğunu kontrol et
        if (!isTotalPriceValid(orderItem)) {
            throw new IllegalStateException(
                String.format("Toplam fiyat hatalı. Beklenen: %s, Mevcut: %s",
                    calculateItemTotal(orderItem), 
                    orderItem.getTotalPrice())
            );
        }
    }

    /**
     * Tüm order item'ları doğrular
     * 
     * @param orderItems doğrulanacak item'lar
     * @throws IllegalStateException herhangi bir item geçersizse
     */
    public void validateOrderItems(List<OrderItem> orderItems) {
        for (OrderItem item : orderItems) {
            validateOrderItem(item);
        }
    }

    /**
     * Order item'ın ağırlıklı yüzdesini hesaplar (toplam içindeki oranı)
     * 
     * @param orderItem hesaplanacak item
     * @param allOrderItems tüm order item'lar
     * @return yüzde değeri (örn: 25.5 = %25.5)
     */
    public BigDecimal calculateItemPercentage(OrderItem orderItem, List<OrderItem> allOrderItems) {
        BigDecimal orderTotal = calculateOrderTotal(allOrderItems);
        
        if (orderTotal.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return orderItem.getTotalPrice()
            .divide(orderTotal, 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }

    /**
     * İndirim uygulanmış fiyat ile orijinal fiyatı karşılaştırır
     * 
     * @param orderItem sipariş item'ı
     * @param currentProductPrice ürünün güncel fiyatı
     * @return indirim yüzdesi (pozitif: indirim var, negatif: zamlandı)
     */
    public BigDecimal calculateDiscountPercentage(OrderItem orderItem, BigDecimal currentProductPrice) {
        if (currentProductPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal difference = currentProductPrice.subtract(orderItem.getUnitPrice());
        return difference
            .divide(currentProductPrice, 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }

    /**
     * En pahalı item'ı bulur
     * 
     * @param orderItems item'lar listesi
     * @return en pahalı item (boş listede null)
     */
    public OrderItem findMostExpensiveItem(List<OrderItem> orderItems) {
        return orderItems.stream()
            .max((item1, item2) -> item1.getTotalPrice().compareTo(item2.getTotalPrice()))
            .orElse(null);
    }

    /**
     * En ucuz item'ı bulur
     * 
     * @param orderItems item'lar listesi
     * @return en ucuz item (boş listede null)
     */
    public OrderItem findLeastExpensiveItem(List<OrderItem> orderItems) {
        return orderItems.stream()
            .min((item1, item2) -> item1.getTotalPrice().compareTo(item2.getTotalPrice()))
            .orElse(null);
    }

    /**
     * Belirli bir üründen kaç adet sipariş edildiğini hesaplar
     * 
     * @param orderItems tüm order item'lar
     * @param product aranan ürün
     * @return toplam miktar
     */
    public int getTotalQuantityByProduct(List<OrderItem> orderItems, Product product) {
        return orderItems.stream()
            .filter(item -> item.getProduct().getId().equals(product.getId()))
            .mapToInt(OrderItem::getQty)
            .sum();
    }

    /**
     * Sipariş özeti oluşturur
     * 
     * @param orderItems sipariş item'ları
     * @return özet bilgiler içeren map
     */
    public Map<String, Object> createOrderSummary(List<OrderItem> orderItems) {
        return Map.of(
            "total_amount", calculateOrderTotal(orderItems),
            "total_items", calculateTotalItemCount(orderItems),
            "unique_products", calculateUniqueProductCount(orderItems),
            "most_expensive_item", findMostExpensiveItem(orderItems),
            "least_expensive_item", findLeastExpensiveItem(orderItems)
        );
    }

    /**
     * Sepeti siparişe dönüştürürken stok kontrolü yapar
     * 
     * @param cart sepet
     * @param cartItems sepet item'ları
     * @param inventoryService stok servisi
     * @throws IllegalStateException yetersiz stok varsa
     */
    public void validateStockAvailability(
        Cart cart, 
        List<CartItem> cartItems,
        InventoryService inventoryService
    ) {
        // Not: Inventory service'den stok kontrolü yapılacak
        // Bu kısım repository inject edildikten sonra implement edilecek
        // Örnek implementasyon:
        // for (CartItem cartItem : cartItems) {
        //     Inventory inventory = inventoryRepository.findByProductId(cartItem.getProduct().getId());
        //     if (!inventoryService.hasAvailableStock(inventory, cartItem.getQty())) {
        //         throw new IllegalStateException("Yetersiz stok: " + cartItem.getProduct().getName());
        //     }
        // }
    }

    /**
     * Order item'ın birim fiyatını günceller ve toplam fiyatı yeniden hesaplar
     * Not: Normalde sipariş sonrası fiyat değişmez, bu method özel durumlar için
     * 
     * @param orderItem güncellenecek item
     * @param newUnitPrice yeni birim fiyat
     * @throws IllegalArgumentException fiyat geçersizse
     */
    public void updateUnitPrice(OrderItem orderItem, BigDecimal newUnitPrice) {
        if (newUnitPrice == null || newUnitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Birim fiyat geçerli olmalıdır");
        }

        orderItem.setUnitPrice(newUnitPrice);
        
        // Total price'ı yeniden hesapla
        BigDecimal newTotalPrice = calculateItemTotal(orderItem);
        orderItem.setTotalPrice(newTotalPrice);
    }

    /**
     * Order item'ın miktarını günceller ve toplam fiyatı yeniden hesaplar
     * Not: Normalde sipariş sonrası miktar değişmez, bu method özel durumlar için
     * 
     * @param orderItem güncellenecek item
     * @param newQuantity yeni miktar
     * @throws IllegalArgumentException miktar geçersizse
     */
    public void updateQuantity(OrderItem orderItem, int newQuantity) {
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("Miktar pozitif olmalıdır");
        }

        orderItem.setQty(newQuantity);
        
        // Total price'ı yeniden hesapla
        BigDecimal newTotalPrice = calculateItemTotal(orderItem);
        orderItem.setTotalPrice(newTotalPrice);
    }
}
