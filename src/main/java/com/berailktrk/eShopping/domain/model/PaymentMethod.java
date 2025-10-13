package com.berailktrk.eShopping.domain.model;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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


//PostgreSQL payment_methods tablosunu temsil eden PaymentMethod entity'si
//Kullanıcıların kayıtlı ödeme yöntemlerini tutar

@Entity
@Table(name = "payment_methods", indexes = {
    @Index(name = "idx_payment_methods_user_id", columnList = "user_id"),
    @Index(name = "idx_payment_methods_is_default", columnList = "is_default"),
    @Index(name = "idx_payment_methods_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    //Ödeme yöntemi adı (kullanıcı tarafından verilen isim)
    //Örnek: "Ana Kartım", "İş Kartı"
    
    @Column(name = "method_name", nullable = false, length = 100)
    private String methodName;

    //Ödeme yöntemi türü
    //Örnek: CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER
    
    @Column(name = "method_type", nullable = false, length = 50)
    private String methodType;

    //Tokenized kart bilgileri (JSON formatında)
    //Gerçek kart bilgileri saklanmaz, sadece token ve maskelenmiş bilgiler
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "card_info", columnDefinition = "jsonb")
    private CardInfo cardInfo;

    //Bu ödeme yöntemi varsayılan mı?
    
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    //Ödeme yöntemi aktif mi?
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 0;
}
