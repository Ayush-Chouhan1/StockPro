package com.stockpro.purchaseservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class GoodsReceiptDTO {

    @NotNull(message = "Line item ID is required")
    private Long lineItemId;

    @NotNull(message = "Received quantity is required")
    @Min(value = 1, message = "Received quantity must be at least 1")
    private Integer receivedQty;
}