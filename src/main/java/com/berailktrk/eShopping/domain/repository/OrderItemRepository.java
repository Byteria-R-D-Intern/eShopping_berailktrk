package com.berailktrk.eShopping.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.berailktrk.eShopping.domain.model.Order;
import com.berailktrk.eShopping.domain.model.OrderItem;

// OrderItem Repository - Sipariş kalemleri işlemleri
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    // Siparişe ait tüm kalemleri getir
    List<OrderItem> findByOrder(Order order);

    // Sipariş ID'ye göre kalemleri getir
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId")
    List<OrderItem> findByOrderId(@Param("orderId") UUID orderId);

    // Ürün ID'ye göre sipariş kalemlerini getir - Analiz ve raporlama için
    @Query("SELECT oi FROM OrderItem oi WHERE oi.product.id = :productId ORDER BY oi.createdAt DESC")
    List<OrderItem> findByProductId(@Param("productId") UUID productId);

    // Belirli bir ürünün toplam satış miktarını hesapla
    @Query("SELECT COALESCE(SUM(oi.qty), 0) FROM OrderItem oi WHERE oi.product.id = :productId")
    Long getTotalQuantitySoldByProductId(@Param("productId") UUID productId);

    // Siparişin toplam kalem sayısını getir
    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.order.id = :orderId")
    Long countByOrderId(@Param("orderId") UUID orderId);

    // Sipariş ID'sine göre tüm kalemleri sil
    void deleteByOrderId(UUID orderId);
}
