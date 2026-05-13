package com.stockpro.reportservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopMovingProductDTO {
    private Long productId;
    private String productName;
    private Long warehouseId;
    private Integer totalUnitsIn;
    private Integer totalUnitsOut;
    private Integer totalMovement;
}