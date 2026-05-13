package com.stockpro.purchaseservice.dto;

import com.stockpro.purchaseservice.entity.POStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderResponseDTO {

    private Long poId;
    private Long supplierId;
    private Long warehouseId;
    private Long createdById;
    private POStatus status;
    private BigDecimal totalAmount;
    private LocalDate orderDate;
    private LocalDate expectedDate;
    private LocalDate receivedDate;
    private String notes;
    private String referenceNumber;
    private LocalDateTime createdAt;
    private List<POLineItemResponseDTO> lineItems;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class POLineItemResponseDTO {
        private Long lineItemId;
        private Long productId;
        private Integer quantity;
        private BigDecimal unitCost;
        private BigDecimal totalCost;
        private Integer receivedQty;
    }
}