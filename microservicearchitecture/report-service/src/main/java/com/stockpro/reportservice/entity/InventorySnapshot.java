package com.stockpro.reportservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_snapshots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventorySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long snapshotId;

    @Column(nullable = false)
    private Long warehouseId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    // quantity * costPrice at time of snapshot
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal stockValue;

    @Column(nullable = false)
    private LocalDate snapshotDate;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (snapshotDate == null) snapshotDate = LocalDate.now();
    }
}