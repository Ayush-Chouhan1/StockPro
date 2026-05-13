package com.stockpro.paymentservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentOrderRequestDTO {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Amount must be at least 1")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters (e.g. INR)")
    private String currency;

    @NotNull(message = "Purchase order ID is required")
    private Long purchaseOrderId;

    @NotNull(message = "User ID is required")
    private Long userId;

    @Size(max = 500, message = "Description max 500 chars")
    private String description;
}