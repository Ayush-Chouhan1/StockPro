package com.stockpro.alertservice.dto;

import com.stockpro.alertservice.entity.AlertType;
import com.stockpro.alertservice.entity.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertResponseDTO {

    private Long alertId;
    private Long recipientId;
    private AlertType type;
    private Severity severity;
    private String title;
    private String message;
    private Long relatedProductId;
    private Long relatedWarehouseId;
    private Long relatedPurchaseOrderId;
    private String channel;
    private String recipientEmail;
    private Boolean isRead;
    private Boolean isAcknowledged;
    private LocalDateTime readAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime createdAt;
}
