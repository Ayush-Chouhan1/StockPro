package com.stockpro.alertservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long alertId;

    @Column(nullable = false)
    private Long recipientId; // User ID who receives this alert

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AlertType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String message;

    @Column
    private Long relatedProductId; // Optional: link to product

    @Column
    private Long relatedWarehouseId; // Optional: link to warehouse

    @Column
    private Long relatedPurchaseOrderId; // Optional: link to PO

    @Column(length = 50)
    private String channel = "IN_APP"; // IN_APP, EMAIL, SMS

    @Column(length = 160)
    private String recipientEmail;

    @Column(nullable = false)
    private Boolean isRead = false;

    @Column(nullable = false)
    private Boolean isAcknowledged = false;

    @Column
    private LocalDateTime readAt;

    @Column
    private LocalDateTime acknowledgedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
