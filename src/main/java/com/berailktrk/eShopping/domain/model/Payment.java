package com.berailktrk.eShopping.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


//PostgreSQL payments tablosunu temsil eden Payment entity'si
//Her ödeme işlemi için ayrı kayıt tutar

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payments_order_id", columnList = "order_id"),
    @Index(name = "idx_payments_user_id", columnList = "user_id"),
    @Index(name = "idx_payments_payment_method_id", columnList = "payment_method_id"),
    @Index(name = "idx_payments_status", columnList = "status"),
    @Index(name = "idx_payments_created_at", columnList = "created_at"),
    @Index(name = "idx_payments_transaction_id", columnList = "transaction_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    //İlgili sipariş
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    //Ödeme yapan kullanıcı
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    //Kullanılan ödeme yöntemi
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;

    //Ödeme tutarı
    
    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    //Para birimi
    
    @Column(nullable = false, length = 3, columnDefinition = "CHAR(3)")
    @Builder.Default
    private String currency = "TRY";

    //Ödeme durumu
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.NONE;

    //Ödeme sağlayıcısından gelen transaction ID
    //Örnek: "txn_1234567890abcdef"
    
    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    //Ödeme sağlayıcısından gelen authorization code
    
    @Column(name = "authorization_code", length = 50)
    private String authorizationCode;

    //Ödeme sağlayıcısından gelen response code
    
    @Column(name = "response_code", length = 10)
    private String responseCode;

    //Ödeme sağlayıcısından gelen response message
    
    @Column(name = "response_message", length = 500)
    private String responseMessage;

    //Ödeme işleminin başlatılma tarihi
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    //Ödeme işleminin tamamlanma tarihi
    
    @Column(name = "completed_at")
    private Instant completedAt;

    //Ödeme işleminin başarısız olma tarihi
    
    @Column(name = "failed_at")
    private Instant failedAt;

    //İade işleminin tarihi
    
    @Column(name = "refunded_at")
    private Instant refundedAt;

    //İade tutarı
    
    @Column(name = "refund_amount", precision = 14, scale = 2)
    private BigDecimal refundAmount;

    //Ödeme sağlayıcısından gelen ek bilgiler (JSON formatında)
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "provider_response", columnDefinition = "jsonb")
    private Map<String, Object> providerResponse;

    //Ödeme ile ilgili ek metadata (JSON formatında)
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 0;
}
