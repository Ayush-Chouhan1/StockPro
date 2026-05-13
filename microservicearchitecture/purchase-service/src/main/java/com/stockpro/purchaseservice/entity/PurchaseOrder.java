package com.stockpro.purchaseservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long poId;

    @Column(nullable = false)
    private Long supplierId;

    @Column(nullable = false)
    private Long warehouseId;

    @Column(nullable = false)
    private Long createdById;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private POStatus status = POStatus.DRAFT;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(updatable = false)
    private LocalDate orderDate;

    private LocalDate expectedDate;

    private LocalDate receivedDate;

    @Column(length = 500)
    private String notes;

    @Column(unique = true, length = 50)
    private String referenceNumber;

    @OneToMany(mappedBy = "purchaseOrder",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.EAGER)
    @Builder.Default
    private List<POLineItem> lineItems = new ArrayList<>();

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        orderDate = LocalDate.now();
        if (status == null) status = POStatus.DRAFT;
    }
}