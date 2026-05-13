package com.stockpro.product_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponseDTO {

    private Long productId;
    private String sku;
    private String name;
    private String description;
    private String category;
    private String brand;
    private String unitOfMeasure;
    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private Integer reorderLevel;
    private Integer maxStockLevel;
    private Integer leadTimeDays;
    private String imageUrl;
    private String barcode;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}