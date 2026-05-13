package com.stockpro.product_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @Column(unique = true, nullable = false, length = 50)
    private String sku;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(length = 100)
    private String brand;

    @Column(nullable = false, length = 50)
    private String unitOfMeasure; // kg, litre, piece, box, etc.

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal costPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal sellingPrice;

    @Column(nullable = false)
    private Integer reorderLevel; // Min quantity before alert

    @Column(nullable = false)
    private Integer maxStockLevel; // Max quantity threshold

    @Column(nullable = false)
    private Integer leadTimeDays; // Days from order to delivery

    @Column(length = 500)
    private String imageUrl;

    @Column(unique = true, length = 100)
    private String barcode;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}