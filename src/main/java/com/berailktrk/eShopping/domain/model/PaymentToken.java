package com.berailktrk.eShopping.domain.model;

import java.time.Instant;
import java.util.Map;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PostgreSQL payment_tokens tablosunu temsil eden PaymentToken entity'si
 */
@Entity
@Table(name = "payment_tokens", indexes = {
    @Index(name = "idx_payment_tokens_user_id", columnList = "user_id"),
    @Index(name = "idx_payment_tokens_provider", columnList = "provider")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String provider;

    @Column(name = "provider_token", nullable = false, columnDefinition = "TEXT")
    private String providerToken;

    @Column(length = 4, columnDefinition = "CHAR(4)")
    private String last4;

    @Column(name = "expiry_month")
    private Integer expiryMonth;

    @Column(name = "expiry_year")
    private Integer expiryYear;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "provider_token_enc", columnDefinition = "bytea")
    private byte[] providerTokenEnc;
}
