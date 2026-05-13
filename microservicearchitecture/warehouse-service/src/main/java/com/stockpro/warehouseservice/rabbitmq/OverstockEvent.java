package com.stockpro.warehouseservice.rabbitmq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverstockEvent implements Serializable {

    private Long productId;
    private Long warehouseId;
    private Integer currentQuantity;
    private Integer maxStockLevel;
    private String productName;
    private String warehouseName;
    private LocalDateTime detectedAt;
}