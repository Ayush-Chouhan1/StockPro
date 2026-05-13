package com.stockpro.purchaseservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class PurchaseOrderRequestDTO {

    @NotNull(message = "Supplier ID is required")
    private Long supplierId;

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    @NotNull(message = "Created by user ID is required")
    private Long createdById;

    @NotNull(message = "At least one line item is required")
    @Size(min = 1, message = "At least one line item is required")
    @Valid
    private List<POLineItemDTO> lineItems;

    private LocalDate expectedDate;

    @Size(max = 500, message = "Notes must be less than 500 characters")
    private String notes;

    @Size(max = 50, message = "Reference number must be less than 50 characters")
    private String referenceNumber;
}