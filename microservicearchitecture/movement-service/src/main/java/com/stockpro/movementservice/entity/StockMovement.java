package com.stockpro.movementservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long movementId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementType movementType;

    @Column(nullable = false)
    private Integer quantity;

    // Reference to PO ID, issue order ID etc.
    private Long referenceId;

    @Column(length = 50)
    private String referenceType;

    @Column(precision = 10, scale = 2)
    private BigDecimal unitCost;

    // User ID who performed this
    @Column(nullable = false)
    private Long performedBy;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime movementDate;

    // Stock balance after this movement
    private Integer balanceAfter;

    @PrePersist
    protected void onCreate() {
        movementDate = LocalDateTime.now();
    }
}