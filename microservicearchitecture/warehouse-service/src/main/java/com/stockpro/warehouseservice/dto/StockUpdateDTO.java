package com.stockpro.warehouseservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class StockUpdateDTO {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    private String binLocation;

    // Optional: thresholds to check for alerts
    private Integer reorderLevel;
    private Integer maxStockLevel;
}