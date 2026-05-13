package com.stockpro.alertservice.rabbitmq;

import com.stockpro.alertservice.dto.AlertRequestDTO;
import com.stockpro.alertservice.entity.AlertType;
import com.stockpro.alertservice.entity.Severity;
import com.stockpro.alertservice.service.AlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AlertEventConsumer {

    @Autowired
    private AlertService alertService;

    // ✅ LISTEN for low stock events from warehouse-service
    @RabbitListener(queues = RabbitMQConfig.LOW_STOCK_QUEUE)
    public void handleLowStock(LowStockEvent event) {
        log.info("Received LOW_STOCK event: product={}, warehouse={}, qty={}",
                event.getProductId(), event.getWarehouseId(),
                event.getCurrentQuantity());

        try {
            AlertRequestDTO dto = new AlertRequestDTO();
            dto.setRecipientId(1L); // Default: notify manager (ID 1)
                                    // In production: look up actual manager ID
            dto.setType(AlertType.LOW_STOCK);
            dto.setSeverity(event.getCurrentQuantity() == 0
                    ? Severity.CRITICAL : Severity.WARNING);
            dto.setTitle("Low Stock Alert");
            dto.setMessage(String.format(
                    "Product '%s' in '%s' is running low. " +
                    "Current: %d units (Reorder level: %d).",
                    event.getProductName(),
                    event.getWarehouseName(),
                    event.getCurrentQuantity(),
                    event.getReorderLevel()));
            dto.setRelatedProductId(event.getProductId());
            dto.setRelatedWarehouseId(event.getWarehouseId());
            dto.setChannel("IN_APP");

            alertService.createAlert(dto);
            log.info("Low stock alert created for product: {}", event.getProductId());

        } catch (Exception e) {
            log.error("Failed to process LOW_STOCK event: {}", e.getMessage(), e);
        }
    }

    // ✅ LISTEN for overstock events from warehouse-service
    @RabbitListener(queues = RabbitMQConfig.OVERSTOCK_QUEUE)
    public void handleOverstock(OverstockEvent event) {
        log.info("Received OVERSTOCK event: product={}, warehouse={}, qty={}",
                event.getProductId(), event.getWarehouseId(),
                event.getCurrentQuantity());

        try {
            AlertRequestDTO dto = new AlertRequestDTO();
            dto.setRecipientId(1L);
            dto.setType(AlertType.OVERSTOCK);
            dto.setSeverity(Severity.WARNING);
            dto.setTitle("Overstock Alert");
            dto.setMessage(String.format(
                    "Product '%s' in '%s' has exceeded max stock level. " +
                    "Current: %d units (Max: %d).",
                    event.getProductName(),
                    event.getWarehouseName(),
                    event.getCurrentQuantity(),
                    event.getMaxStockLevel()));
            dto.setRelatedProductId(event.getProductId());
            dto.setRelatedWarehouseId(event.getWarehouseId());
            dto.setChannel("IN_APP");

            alertService.createAlert(dto);
            log.info("Overstock alert created for product: {}", event.getProductId());

        } catch (Exception e) {
            log.error("Failed to process OVERSTOCK event: {}", e.getMessage(), e);
        }
    }

    // ✅ LISTEN for PO approved events from purchase-service
    @RabbitListener(queues = RabbitMQConfig.PO_PENDING_QUEUE)
    public void handlePOPendingApproval(POPendingApprovalEvent event) {
        log.info("Received PO_PENDING_APPROVAL event: PO={}", event.getPoId());

        try {
            AlertRequestDTO dto = new AlertRequestDTO();
            dto.setRecipientId(1L);
            dto.setType(AlertType.PO_PENDING_APPROVAL);
            dto.setSeverity(Severity.WARNING);
            dto.setTitle("Purchase Order Pending Approval");
            dto.setMessage(String.format(
                    "PO #%d is waiting for approval. Total amount: %.2f. Expected delivery: %s.",
                    event.getPoId(),
                    event.getTotalAmount(),
                    event.getExpectedDate()));
            dto.setRelatedPurchaseOrderId(event.getPoId());
            dto.setChannel("IN_APP,EMAIL");

            alertService.createAlert(dto);
            log.info("PO pending approval alert created for PO: {}", event.getPoId());

        } catch (Exception e) {
            log.error("Failed to process PO_PENDING_APPROVAL event: {}",
                    e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.PO_APPROVED_QUEUE)
    public void handlePOApproved(POApprovedEvent event) {
        log.info("Received PO_APPROVED event: PO={}, supplier={}",
                event.getPoId(), event.getSupplierId());

        try {
            AlertRequestDTO dto = new AlertRequestDTO();
            dto.setRecipientId(event.getApprovedByUserId());
            dto.setType(AlertType.PO_PENDING_APPROVAL);
            dto.setSeverity(Severity.INFO);
            dto.setTitle("Purchase Order Approved");
            dto.setMessage(String.format(
                    "PO #%d has been approved. Total amount: ₹%.2f. " +
                    "Expected delivery: %s.",
                    event.getPoId(),
                    event.getTotalAmount(),
                    event.getExpectedDate()));
            dto.setRelatedPurchaseOrderId(event.getPoId());
            dto.setChannel("IN_APP");

            alertService.createAlert(dto);
            log.info("PO approved alert created for PO: {}", event.getPoId());

        } catch (Exception e) {
            log.error("Failed to process PO_APPROVED event: {}", e.getMessage(), e);
        }
    }

    // ✅ LISTEN for overdue PO events from purchase-service
    @RabbitListener(queues = RabbitMQConfig.PO_OVERDUE_QUEUE)
    public void handlePOOverdue(POOverdueEvent event) {
        log.info("Received PO_OVERDUE event: PO={}, {} days overdue",
                event.getPoId(), event.getDaysOverdue());

        try {
            AlertRequestDTO dto = new AlertRequestDTO();
            dto.setRecipientId(1L);
            dto.setType(AlertType.OVERDUE_RECEIPT);
            dto.setSeverity(Severity.CRITICAL);
            dto.setTitle("Overdue Purchase Order");
            dto.setMessage(String.format(
                    "PO #%d is overdue by %d days! " +
                    "Expected delivery was %s. Please follow up with supplier.",
                    event.getPoId(),
                    event.getDaysOverdue(),
                    event.getExpectedDate()));
            dto.setRelatedPurchaseOrderId(event.getPoId());
            dto.setChannel("IN_APP");

            alertService.createAlert(dto);
            log.info("Overdue PO alert created for PO: {}", event.getPoId());

        } catch (Exception e) {
            log.error("Failed to process PO_OVERDUE event: {}", e.getMessage(), e);
        }
    }
}
