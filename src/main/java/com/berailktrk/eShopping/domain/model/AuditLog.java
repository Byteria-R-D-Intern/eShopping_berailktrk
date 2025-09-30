package com.berailktrk.eShopping.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.Immutable;
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
 * PostgreSQL audit_logs tablosunu temsil eden AuditLog entity'si
 * 
 * ÖNEMLİ: Bu tablo APPEND-ONLY'dir!
 * - Sadece INSERT yapılabilir
 * - UPDATE ve DELETE database trigger'ları tarafından engellenir
 * - Immutable olarak işaretlenmiştir
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_logs_actor_user_id", columnList = "actor_user_id"),
    @Index(name = "idx_audit_logs_action_type", columnList = "action_type"),
    @Index(name = "idx_audit_logs_resource_type_id", columnList = "resource_type, resource_id"),
    @Index(name = "idx_audit_logs_created_at", columnList = "created_at")
})
@Immutable  // Hibernate'e bu entity'nin değiştirilemez olduğunu söyler
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actorUser;

    @Column(name = "action_type", nullable = false, columnDefinition = "TEXT")
    private String actionType;

    @Column(name = "resource_type", columnDefinition = "TEXT")
    private String resourceType;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
