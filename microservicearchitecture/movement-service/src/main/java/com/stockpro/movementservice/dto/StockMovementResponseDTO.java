package com.stockpro.movementservice.dto;

import com.stockpro.movementservice.entity.MovementType;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovementResponseDTO {

    private Long movementId;
    private Long productId;
    private Long warehouseId;
    private MovementType movementType;
    private Integer quantity;
    private Long referenceId;
    private String referenceType;
    private BigDecimal unitCost;
    private Long performedBy;
    private String notes;
    private LocalDateTime movementDate;
    private Integer balanceAfter;
}