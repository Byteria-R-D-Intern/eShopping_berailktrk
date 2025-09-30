package com.berailktrk.eShopping.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.berailktrk.eShopping.domain.model.Order;
import com.berailktrk.eShopping.domain.model.OrderStatus;
import com.berailktrk.eShopping.domain.model.PaymentStatus;
import com.berailktrk.eShopping.domain.model.User;

/**
 * Order repository interface
 * Sipariş yönetimi ve sorgulama işlemleri için repository
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Kullanıcının tüm siparişlerini getir
     * 
     * @param user kullanıcı
     * @param pageable sayfalama bilgisi
     * @return siparişler
     */
    Page<Order> findByUser(User user, Pageable pageable);

    /**
     * Kullanıcı ID'ye göre siparişleri getir
     * 
     * @param userId kullanıcı ID
     * @param pageable sayfalama bilgisi
     * @return siparişler
     */
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
    Page<Order> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Kullanıcının belirli durumdaki siparişlerini getir
     * 
     * @param userId kullanıcı ID
     * @param status sipariş durumu
     * @return siparişler
     */
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.status = :status ORDER BY o.createdAt DESC")
    List<Order> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") OrderStatus status);

    /**
     * Durum bazlı siparişleri getir
     * 
     * @param status sipariş durumu
     * @param pageable sayfalama bilgisi
     * @return siparişler
     */
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    /**
     * Ödeme durumuna göre siparişleri getir
     * 
     * @param paymentStatus ödeme durumu
     * @param pageable sayfalama bilgisi
     * @return siparişler
     */
    Page<Order> findByPaymentStatus(PaymentStatus paymentStatus, Pageable pageable);

    /**
     * Sipariş numarasına göre sipariş bul
     * 
     * @param orderNumber sipariş numarası
     * @return sipariş (varsa)
     */
    Optional<Order> findByOrderNumber(Long orderNumber);

    /**
     * Belirli tarih aralığındaki siparişleri getir
     * 
     * @param startDate başlangıç tarihi
     * @param endDate bitiş tarihi
     * @param pageable sayfalama bilgisi
     * @return siparişler
     */
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate ORDER BY o.createdAt DESC")
    Page<Order> findByDateRange(@Param("startDate") Instant startDate, 
                                 @Param("endDate") Instant endDate, 
                                 Pageable pageable);

    /**
     * Kullanıcının son N siparişini getir
     * 
     * @param userId kullanıcı ID
     * @param limit limit sayısı
     * @return siparişler
     */
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
    List<Order> findRecentOrdersByUserId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Pending durumundaki eski siparişleri getir (timeout kontrolü için)
     * 
     * @param threshold zaman eşiği
     * @return eski pending siparişler
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING' AND o.createdAt < :threshold")
    List<Order> findStalePendingOrders(@Param("threshold") Instant threshold);

    /**
     * Kullanıcının toplam sipariş sayısını getir
     * 
     * @param userId kullanıcı ID
     * @return sipariş sayısı
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId")
    Long countByUserId(@Param("userId") UUID userId);

    /**
     * Durum ve ödeme durumuna göre siparişleri getir
     * 
     * @param status sipariş durumu
     * @param paymentStatus ödeme durumu
     * @return siparişler
     */
    List<Order> findByStatusAndPaymentStatus(OrderStatus status, PaymentStatus paymentStatus);
}
