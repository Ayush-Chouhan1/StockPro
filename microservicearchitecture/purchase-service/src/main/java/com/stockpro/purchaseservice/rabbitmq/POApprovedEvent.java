package com.stockpro.purchaseservice.rabbitmq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class POApprovedEvent implements Serializable {

    private Long poId;
    private Long supplierId;
    private Long warehouseId;
    private Long approvedByUserId;
    private BigDecimal totalAmount;
    private LocalDate expectedDate;
    private LocalDateTime approvedAt;
}