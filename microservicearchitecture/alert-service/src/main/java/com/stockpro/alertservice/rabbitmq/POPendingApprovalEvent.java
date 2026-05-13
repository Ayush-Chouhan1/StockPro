package com.stockpro.alertservice.rabbitmq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class POPendingApprovalEvent {
    private Long poId;
    private Long supplierId;
    private Long warehouseId;
    private Long requestedByUserId;
    private BigDecimal totalAmount;
    private LocalDate expectedDate;
    private LocalDateTime submittedAt;
}
