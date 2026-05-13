package com.stockpro.warehouseservice.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockLevelResponseDTO {

    private Long stockId;
    private Long warehouseId;
    private Long productId;
    private Integer quantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private String binLocation;
    private LocalDateTime lastUpdated;
}