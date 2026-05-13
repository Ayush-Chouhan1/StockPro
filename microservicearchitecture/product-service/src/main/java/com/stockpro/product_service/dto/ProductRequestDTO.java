package com.stockpro.product_service.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequestDTO {

    @NotBlank(message = "SKU is required")
    @Size(max = 50, message = "SKU must be less than 50 characters")
    private String sku;

    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Name must be less than 200 characters")
    private String name;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    private String brand;

    @NotBlank(message = "Unit of measure is required")
    private String unitOfMeasure;

    @NotNull(message = "Cost price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Cost price must be greater than 0")
    private BigDecimal costPrice;

    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Selling price must be greater than 0")
    private BigDecimal sellingPrice;

    @NotNull(message = "Reorder level is required")
    @Min(value = 0, message = "Reorder level cannot be negative")
    private Integer reorderLevel;

    @NotNull(message = "Max stock level is required")
    @Min(value = 1, message = "Max stock level must be at least 1")
    private Integer maxStockLevel;

    @NotNull(message = "Lead time is required")
    @Min(value = 1, message = "Lead time must be at least 1 day")
    private Integer leadTimeDays;

    private String imageUrl;

    private String barcode;
}