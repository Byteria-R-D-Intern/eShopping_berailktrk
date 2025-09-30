package com.berailktrk.eShopping.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.berailktrk.eShopping.domain.model.Product;

import jakarta.persistence.LockModeType;

/**
 * Product repository interface
 * Ürün CRUD ve sorgulama işlemleri için repository
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    /**
     * SKU'ya göre ürün bul
     * 
     * @param sku ürün SKU
     * @return ürün (varsa)
     */
    Optional<Product> findBySku(String sku);

    /**
     * SKU'nun var olup olmadığını kontrol et
     * 
     * @param sku kontrol edilecek SKU
     * @return varsa true
     */
    boolean existsBySku(String sku);

    /**
     * Aktif ürünleri getir
     * 
     * @return aktif ürün listesi
     */
    List<Product> findByIsActiveTrue();

    /**
     * İsme göre ürün ara (case-insensitive, partial match)
     * 
     * @param name aranacak isim
     * @return bulunan ürünler
     */
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) AND p.isActive = true")
    List<Product> searchByName(@Param("name") String name);

    /**
     * Ürünü pessimistic lock ile getir (stok işlemleri için)
     * 
     * @param id ürün ID
     * @return ürün (varsa)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") UUID id);

    /**
     * SKU ile ürünü pessimistic lock ile getir
     * 
     * @param sku ürün SKU
     * @return ürün (varsa)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.sku = :sku")
    Optional<Product> findBySkuWithLock(@Param("sku") String sku);
}
