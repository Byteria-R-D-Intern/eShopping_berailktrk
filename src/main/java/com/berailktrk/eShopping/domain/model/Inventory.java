package com.berailktrk.eShopping.domain.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PostgreSQL inventory tablosunu temsil eden Inventory entity'si
 */
@Entity
@Table(name = "inventory", indexes = {
    @Index(name = "idx_inventory_quantity", columnList = "quantity"),
    @Index(name = "idx_inventory_updated_at", columnList = "updated_at"),
    @Index(name = "idx_inventory_product_sku", columnList = "product_sku"),
    @Index(name = "idx_inventory_product_name", columnList = "product_name")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "product_sku", nullable = false, columnDefinition = "TEXT")
    private String productSku;

    @Column(name = "product_name", nullable = false, columnDefinition = "TEXT")
    private String productName;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer reserved = 0;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Version
    @Column(nullable = false)
    private Integer version;
}