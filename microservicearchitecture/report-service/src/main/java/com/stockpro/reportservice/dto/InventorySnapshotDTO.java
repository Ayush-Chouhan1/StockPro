package com.stockpro.reportservice.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventorySnapshotDTO {
    private Long snapshotId;
    private Long warehouseId;
    private Long productId;
    private Integer quantity;
    private BigDecimal stockValue;
    private LocalDate snapshotDate;
    private LocalDateTime createdAt;
}