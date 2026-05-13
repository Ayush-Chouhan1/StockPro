package com.stockpro.purchaseservice.rabbitmq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class POOverdueEvent implements Serializable {

    private Long poId;
    private Long supplierId;
    private Long warehouseId;
    private LocalDate expectedDate;
    private long daysOverdue;
    private LocalDateTime detectedAt;
}