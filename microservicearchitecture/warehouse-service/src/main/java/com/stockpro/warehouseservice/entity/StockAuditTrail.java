package com.stockpro.warehouseservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_audit_trail")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAuditTrail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long auditId;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(nullable = false)
    private Long productId;

    private Long warehouseId;

    private Long sourceWarehouseId;

    private Long destinationWarehouseId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 160)
    private String actor;

    @Lob
    private String details;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
