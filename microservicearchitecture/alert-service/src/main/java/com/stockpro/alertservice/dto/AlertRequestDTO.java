package com.stockpro.alertservice.dto;

import com.stockpro.alertservice.entity.AlertType;
import com.stockpro.alertservice.entity.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AlertRequestDTO {

    @NotNull(message = "Recipient ID is required")
    private Long recipientId;

    @NotNull(message = "Alert type is required")
    private AlertType type;

    @NotNull(message = "Severity is required")
    private Severity severity;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    private Long relatedProductId;
    private Long relatedWarehouseId;
    private Long relatedPurchaseOrderId;
    private String channel = "IN_APP";
    private String recipientEmail;
}
