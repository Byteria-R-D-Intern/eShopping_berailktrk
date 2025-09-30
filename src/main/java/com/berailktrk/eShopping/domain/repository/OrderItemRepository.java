package com.berailktrk.eShopping.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.berailktrk.eShopping.domain.model.Order;
import com.berailktrk.eShopping.domain.model.OrderItem;

/**
 * OrderItem repository interface
 * Sipariş kalemleri için repository
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    /**
     * Siparişe ait tüm kalemleri getir
     * 
     * @param order sipariş
     * @return sipariş kalemleri
     */
    List<OrderItem> findByOrder(Order order);

    /**
     * Sipariş ID'ye göre kalemleri getir
     * 
     * @param orderId sipariş ID
     * @return sipariş kalemleri
     */
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId")
    List<OrderItem> findByOrderId(@Param("orderId") UUID orderId);

    /**
     * Ürün ID'ye göre sipariş kalemlerini getir
     * Ürün analizi ve raporlama için kullanılır
     * 
     * @param productId ürün ID
     * @return sipariş kalemleri
     */
    @Query("SELECT oi FROM OrderItem oi WHERE oi.product.id = :productId ORDER BY oi.createdAt DESC")
    List<OrderItem> findByProductId(@Param("productId") UUID productId);

    /**
     * Belirli bir ürünün toplam satış miktarını hesapla
     * 
     * @param productId ürün ID
     * @return toplam satış miktarı
     */
    @Query("SELECT COALESCE(SUM(oi.qty), 0) FROM OrderItem oi WHERE oi.product.id = :productId")
    Long getTotalQuantitySoldByProductId(@Param("productId") UUID productId);

    /**
     * Siparişin toplam kalem sayısını getir
     * 
     * @param orderId sipariş ID
     * @return kalem sayısı
     */
    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.order.id = :orderId")
    Long countByOrderId(@Param("orderId") UUID orderId);

    /**
     * Sipariş ID'sine göre tüm kalemleri sil
     * 
     * @param orderId sipariş ID
     */
    void deleteByOrderId(UUID orderId);
}
