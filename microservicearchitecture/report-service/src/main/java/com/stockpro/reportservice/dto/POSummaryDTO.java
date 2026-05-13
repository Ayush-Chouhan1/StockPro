package com.stockpro.reportservice.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class POSummaryDTO {
    private LocalDate fromDate;
    private LocalDate toDate;
    private Long totalPOs;
    private BigDecimal totalSpend;
    private Long approvedPOs;
    private Long pendingPOs;
    private Long cancelledPOs;
    private Long fullyReceivedPOs;
}