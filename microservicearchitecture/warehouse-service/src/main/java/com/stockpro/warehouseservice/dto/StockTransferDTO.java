package com.stockpro.warehouseservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class StockTransferDTO {

    @NotNull(message = "Source warehouse ID is required")
    private Long fromWarehouseId;

    @NotNull(message = "Destination warehouse ID is required")
    private Long toWarehouseId;

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Transfer quantity must be at least 1")
    private Integer quantity;

    @NotBlank(message = "Reason is required")
    private String reason;
}