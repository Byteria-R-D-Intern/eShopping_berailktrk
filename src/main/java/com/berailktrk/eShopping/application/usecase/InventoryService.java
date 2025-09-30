package com.berailktrk.eShopping.application.usecase;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.berailktrk.eShopping.domain.model.Inventory;

/**
 * Inventory entity'si için uygulama servis katmanı
 * Stok yönetimi ve karmaşık business logic'i yönetir
 */
@Service
public class InventoryService {

    /**
     * Mevcut stok miktarını (rezerve edilmemiş) hesaplar
     * 
     * @param inventory kontrol edilecek envanter
     * @return müsait stok miktarı
     */
    public int getAvailableQuantity(Inventory inventory) {
        return inventory.getQuantity() - inventory.getReserved();
    }

    /**
     * Belirtilen miktarda stok olup olmadığını kontrol eder
     * 
     * @param inventory kontrol edilecek envanter
     * @param requestedQuantity talep edilen miktar
     * @return yeterli stok varsa true, yoksa false
     */
    public boolean hasAvailableStock(Inventory inventory, int requestedQuantity) {
        if (requestedQuantity < 0) {
            throw new IllegalArgumentException("Talep edilen miktar negatif olamaz");
        }
        return getAvailableQuantity(inventory) >= requestedQuantity;
    }

    /**
     * Stok rezerve eder (sipariş oluşturma sırasında)
     * 
     * @param inventory güncellenecek envanter
     * @param quantityToReserve rezerve edilecek miktar
     * @throws IllegalStateException yeterli stok yoksa
     * @throws IllegalArgumentException miktar geçersizse
     */
    public void reserveStock(Inventory inventory, int quantityToReserve) {
        if (quantityToReserve <= 0) {
            throw new IllegalArgumentException("Rezerve edilecek miktar pozitif olmalıdır");
        }

        if (!hasAvailableStock(inventory, quantityToReserve)) {
            throw new IllegalStateException(
                String.format("Yetersiz stok. Mevcut: %d, Talep: %d", 
                    getAvailableQuantity(inventory), quantityToReserve)
            );
        }

        inventory.setReserved(inventory.getReserved() + quantityToReserve);
        inventory.setUpdatedAt(Instant.now());
    }

    /**
     * Rezerve edilmiş stoku serbest bırakır (sipariş iptal edildiğinde)
     * 
     * @param inventory güncellenecek envanter
     * @param quantityToRelease serbest bırakılacak miktar
     * @throws IllegalArgumentException miktar geçersizse veya rezerve miktardan fazlaysa
     */
    public void releaseReservedStock(Inventory inventory, int quantityToRelease) {
        if (quantityToRelease <= 0) {
            throw new IllegalArgumentException("Serbest bırakılacak miktar pozitif olmalıdır");
        }

        if (quantityToRelease > inventory.getReserved()) {
            throw new IllegalArgumentException(
                String.format("Serbest bırakılacak miktar (%d), rezerve miktardan (%d) fazla olamaz",
                    quantityToRelease, inventory.getReserved())
            );
        }

        inventory.setReserved(inventory.getReserved() - quantityToRelease);
        inventory.setUpdatedAt(Instant.now());
    }

    /**
     * Stok miktarını artırır (yeni ürün geldiğinde)
     * 
     * @param inventory güncellenecek envanter
     * @param quantityToAdd eklenecek miktar
     * @throws IllegalArgumentException miktar geçersizse
     */
    public void addStock(Inventory inventory, int quantityToAdd) {
        if (quantityToAdd <= 0) {
            throw new IllegalArgumentException("Eklenecek miktar pozitif olmalıdır");
        }

        inventory.setQuantity(inventory.getQuantity() + quantityToAdd);
        inventory.setUpdatedAt(Instant.now());
    }

    /**
     * Rezerve edilmiş stoku teyit eder ve toplam stoktan düşer (sipariş tamamlandığında)
     * 
     * @param inventory güncellenecek envanter
     * @param quantityToConfirm teyit edilecek miktar
     * @throws IllegalArgumentException miktar geçersizse
     */
    public void confirmReservedStock(Inventory inventory, int quantityToConfirm) {
        if (quantityToConfirm <= 0) {
            throw new IllegalArgumentException("Teyit edilecek miktar pozitif olmalıdır");
        }

        if (quantityToConfirm > inventory.getReserved()) {
            throw new IllegalArgumentException(
                String.format("Teyit edilecek miktar (%d), rezerve miktardan (%d) fazla olamaz",
                    quantityToConfirm, inventory.getReserved())
            );
        }

        inventory.setQuantity(inventory.getQuantity() - quantityToConfirm);
        inventory.setReserved(inventory.getReserved() - quantityToConfirm);
        inventory.setUpdatedAt(Instant.now());
    }

    /**
     * Depo konumunu günceller
     * 
     * @param inventory güncellenecek envanter
     * @param warehouseLocation yeni depo konumu
     */
    public void updateWarehouseLocation(Inventory inventory, String warehouseLocation) {
        inventory.setWarehouseLocation(warehouseLocation);
        inventory.setUpdatedAt(Instant.now());
    }

    /**
     * Stok durumunun geçerli olup olmadığını kontrol eder
     * 
     * @param inventory kontrol edilecek envanter
     * @throws IllegalStateException stok durumu geçersizse
     */
    public void validateInventoryState(Inventory inventory) {
        if (inventory.getQuantity() < 0) {
            throw new IllegalStateException("Stok miktarı negatif olamaz");
        }

        if (inventory.getReserved() < 0) {
            throw new IllegalStateException("Rezerve miktar negatif olamaz");
        }

        if (inventory.getReserved() > inventory.getQuantity()) {
            throw new IllegalStateException(
                String.format("Rezerve miktar (%d), toplam stok miktarından (%d) fazla olamaz",
                    inventory.getReserved(), inventory.getQuantity())
            );
        }
    }

    /**
     * Stokta ürün var mı kontrol eder
     * 
     * @param inventory kontrol edilecek envanter
     * @return stokta varsa true, yoksa false
     */
    public boolean isInStock(Inventory inventory) {
        return getAvailableQuantity(inventory) > 0;
    }

    /**
     * Stok kritik seviyede mi kontrol eder
     * 
     * @param inventory kontrol edilecek envanter
     * @param threshold eşik değeri
     * @return stok eşik değerinin altındaysa true
     */
    public boolean isLowStock(Inventory inventory, int threshold) {
        if (threshold < 0) {
            throw new IllegalArgumentException("Eşik değeri negatif olamaz");
        }
        return getAvailableQuantity(inventory) < threshold;
    }

    /**
     * Stok tükendi mi kontrol eder
     * 
     * @param inventory kontrol edilecek envanter
     * @return stok tükendiyse true
     */
    public boolean isOutOfStock(Inventory inventory) {
        return getAvailableQuantity(inventory) <= 0;
    }
}
