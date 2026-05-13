package com.stockpro.reportservice.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockValuationDTO {
    private Long warehouseId;
    private BigDecimal totalValue;
    private LocalDate asOfDate;
    private Integer totalProducts;
}