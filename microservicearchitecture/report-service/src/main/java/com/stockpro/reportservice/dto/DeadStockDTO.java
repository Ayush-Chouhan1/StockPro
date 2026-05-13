package com.stockpro.reportservice.dto;

import lombok.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeadStockDTO {
    private Long productId;
    private Long warehouseId;
    private Integer currentQuantity;
    private LocalDate lastMovementDate;
    private Integer daysWithoutMovement;
}