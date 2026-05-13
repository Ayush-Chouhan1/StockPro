package com.stockpro.paymentservice.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentOrderResponseDTO {

    private Long paymentId;
    private String razorpayOrderId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String description;

    // Razorpay key for frontend checkout
    private String razorpayKeyId;
}