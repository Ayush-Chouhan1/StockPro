package com.stockpro.movementservice.dto;

import com.stockpro.movementservice.entity.MovementType;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class StockMovementRequestDTO {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    @NotNull(message = "Movement type is required")
    private MovementType movementType;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    private Long referenceId;

    @Size(max = 50, message = "Reference type max 50 chars")
    private String referenceType;

    @DecimalMin(value = "0.0", message = "Unit cost cannot be negative")
    private BigDecimal unitCost;

    @NotNull(message = "Performed by user ID is required")
    private Long performedBy;

    @Size(max = 500, message = "Notes max 500 chars")
    private String notes;

    @NotNull(message = "Balance after movement is required")
    private Integer balanceAfter;
}