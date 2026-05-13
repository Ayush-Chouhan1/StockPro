package com.stockpro.purchaseservice.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
@Slf4j
public class POEventPublisher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void publishPOPendingApproval(Long poId, Long supplierId,
            Long warehouseId, Long requestedByUserId,
            java.math.BigDecimal totalAmount, LocalDate expectedDate) {

        POPendingApprovalEvent event = POPendingApprovalEvent.builder()
                .poId(poId)
                .supplierId(supplierId)
                .warehouseId(warehouseId)
                .requestedByUserId(requestedByUserId)
                .totalAmount(totalAmount)
                .expectedDate(expectedDate)
                .submittedAt(LocalDateTime.now())
                .build();

        log.info("Publishing PO_PENDING_APPROVAL event for PO: {}", poId);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.PO_PENDING_KEY,
                event);
    }

    public void publishPOApproved(Long poId, Long supplierId,
            Long warehouseId, Long approvedByUserId,
            java.math.BigDecimal totalAmount, LocalDate expectedDate) {

        POApprovedEvent event = POApprovedEvent.builder()
                .poId(poId)
                .supplierId(supplierId)
                .warehouseId(warehouseId)
                .approvedByUserId(approvedByUserId)
                .totalAmount(totalAmount)
                .expectedDate(expectedDate)
                .approvedAt(LocalDateTime.now())
                .build();

        log.info("Publishing PO_APPROVED event for PO: {}", poId);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.PO_APPROVED_KEY,
                event);

        log.info("PO_APPROVED event published for PO: {}", poId);
    }

    public void publishPOOverdue(Long poId, Long supplierId,
            Long warehouseId, LocalDate expectedDate) {

        long daysOverdue = ChronoUnit.DAYS.between(
                expectedDate, LocalDate.now());

        POOverdueEvent event = POOverdueEvent.builder()
                .poId(poId)
                .supplierId(supplierId)
                .warehouseId(warehouseId)
                .expectedDate(expectedDate)
                .daysOverdue(daysOverdue)
                .detectedAt(LocalDateTime.now())
                .build();

        log.info("Publishing PO_OVERDUE event for PO: {} ({} days overdue)",
                poId, daysOverdue);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.PO_OVERDUE_KEY,
                event);
    }
}
