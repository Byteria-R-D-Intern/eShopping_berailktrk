package com.berailktrk.eShopping.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.berailktrk.eShopping.domain.model.Payment;
import com.berailktrk.eShopping.domain.model.PaymentStatus;


//Payment entity için repository interface

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    //Belirli bir siparişe ait ödemeleri getir
    
    List<Payment> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    //Belirli bir kullanıcıya ait ödemeleri getir
    
    List<Payment> findByUserIdOrderByCreatedAtDesc(UUID userId);

    //Belirli bir siparişe ait aktif ödemeyi getir
    
    @Query("SELECT p FROM Payment p WHERE p.order.id = :orderId AND p.status IN :statuses ORDER BY p.createdAt DESC")
    Optional<Payment> findActivePaymentByOrderId(@Param("orderId") UUID orderId, 
                                                  @Param("statuses") List<PaymentStatus> statuses);

    //Transaction ID ile ödeme bul
    
    Optional<Payment> findByTransactionId(String transactionId);

    //Belirli bir durumdaki ödemeleri getir
    
    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);

    //Belirli bir kullanıcının belirli bir durumdaki ödemelerini getir    
    List<Payment> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, PaymentStatus status);
}
