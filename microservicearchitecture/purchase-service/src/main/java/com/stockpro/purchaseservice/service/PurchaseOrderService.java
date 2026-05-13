package com.stockpro.purchaseservice.service;

import com.stockpro.purchaseservice.dto.*;
import com.stockpro.purchaseservice.dto.PurchaseOrderResponseDTO.POLineItemResponseDTO;
import com.stockpro.purchaseservice.entity.*;
import com.stockpro.purchaseservice.exception.*;
import com.stockpro.purchaseservice.rabbitmq.POEventPublisher;
import com.stockpro.purchaseservice.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class PurchaseOrderService {

    @Autowired
    private PurchaseOrderRepository poRepository;

    @Autowired
    private POLineItemRepository lineItemRepository;

    @Autowired
    private POEventPublisher poEventPublisher;

    @Autowired
    private POAuditService poAuditService;

    @Autowired(required = false)
    private RestTemplate restTemplate;

    @Value("${services.warehouse-url:http://localhost:8084}")
    private String warehouseUrl;

    @Value("${services.report-url:http://localhost:8089}")
    private String reportUrl;

    @Value("${services.movement-url:http://localhost:8087}")
    private String movementUrl;

    @Transactional
    public PurchaseOrderResponseDTO createPO(PurchaseOrderRequestDTO dto) {
        log.info("Creating PO for supplier: {}", dto.getSupplierId());

        PurchaseOrder po = PurchaseOrder.builder()
                .supplierId(dto.getSupplierId())
                .warehouseId(dto.getWarehouseId())
                .createdById(dto.getCreatedById())
                .expectedDate(dto.getExpectedDate())
                .notes(dto.getNotes())
                .referenceNumber(dto.getReferenceNumber())
                .status(POStatus.DRAFT)
                .build();

        List<POLineItem> items = dto.getLineItems().stream()
                .map(itemDto -> {
                    BigDecimal total = itemDto.getUnitCost()
                            .multiply(BigDecimal.valueOf(itemDto.getQuantity()));
                    return POLineItem.builder()
                            .productId(itemDto.getProductId())
                            .quantity(itemDto.getQuantity())
                            .unitCost(itemDto.getUnitCost())
                            .totalCost(total)
                            .receivedQty(0)
                            .purchaseOrder(po)
                            .build();
                }).toList();

        po.setLineItems(items);

        BigDecimal totalAmount = items.stream()
                .map(POLineItem::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        po.setTotalAmount(totalAmount);

        PurchaseOrder saved = poRepository.save(po);
        audit(saved.getPoId(), null, saved.getStatus(),
                "CREATE_PO", saved.getCreatedById(), "Purchase order created");
        log.info("PO created with ID: {}", saved.getPoId());
        return mapToDTO(saved);
    }

    public PurchaseOrderResponseDTO getPOById(Long id) {
        PurchaseOrder po = poRepository.findById(id)
                .orElseThrow(() -> new PurchaseOrderNotFoundException(
                        "Purchase order not found with ID: " + id));
        return mapToDTO(po);
    }

    public List<PurchaseOrderResponseDTO> getAllPOs() {
        return poRepository.findAll().stream().map(this::mapToDTO).toList();
    }

    public List<PurchaseOrderResponseDTO> getPOsBySupplier(Long supplierId) {
        return poRepository.findBySupplierId(supplierId)
                .stream().map(this::mapToDTO).toList();
    }

    public List<PurchaseOrderResponseDTO> getPOsByWarehouse(Long warehouseId) {
        return poRepository.findByWarehouseId(warehouseId)
                .stream().map(this::mapToDTO).toList();
    }

    public List<PurchaseOrderResponseDTO> getPOsByStatus(String status) {
        try {
            POStatus poStatus = POStatus.valueOf(status.toUpperCase());
            return poRepository.findByStatus(poStatus)
                    .stream().map(this::mapToDTO).toList();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
    }

    public List<PurchaseOrderResponseDTO> getPOsByCreatedBy(Long userId) {
        return poRepository.findByCreatedById(userId)
                .stream().map(this::mapToDTO).toList();
    }

    public List<PurchaseOrderResponseDTO> getPOsByDateRange(
            LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Start date cannot be after end date");
        }
        return poRepository.findByOrderDateBetween(startDate, endDate)
                .stream().map(this::mapToDTO).toList();
    }

    public List<PurchaseOrderResponseDTO> getOverduePOs() {
        return poRepository.findByStatusAndExpectedDateBefore(
                        POStatus.APPROVED, LocalDate.now())
                .stream().map(this::mapToDTO).toList();
    }

    @Transactional
    public PurchaseOrderResponseDTO submitForApproval(Long id) {
        PurchaseOrder po = getPOEntity(id);
        if (po.getStatus() != POStatus.DRAFT) {
            throw new InvalidPOStatusException(
                    "Only DRAFT POs can be submitted. Current: " + po.getStatus());
        }
        POStatus previousStatus = po.getStatus();
        po.setStatus(POStatus.PENDING);
        PurchaseOrder saved = poRepository.save(po);
        audit(saved.getPoId(), previousStatus, saved.getStatus(),
                "SUBMIT_PO", saved.getCreatedById(), "Submitted for approval");
        try {
            poEventPublisher.publishPOPendingApproval(
                    saved.getPoId(),
                    saved.getSupplierId(),
                    saved.getWarehouseId(),
                    saved.getCreatedById(),
                    saved.getTotalAmount(),
                    saved.getExpectedDate());
        } catch (Exception e) {
            log.error("Failed to publish PO_PENDING_APPROVAL event: {}",
                    e.getMessage());
        }
        return mapToDTO(saved);
    }

    @Transactional
    public PurchaseOrderResponseDTO approvePO(Long id) {
        log.info("Approving PO: {}", id);
        PurchaseOrder po = getPOEntity(id);

        if (po.getStatus() != POStatus.PENDING) {
            throw new InvalidPOStatusException(
                    "Only PENDING POs can be approved. Current: " + po.getStatus());
        }

        POStatus previousStatus = po.getStatus();
        po.setStatus(POStatus.APPROVED);
        PurchaseOrder saved = poRepository.save(po);
        audit(saved.getPoId(), previousStatus, saved.getStatus(),
                "APPROVE_PO", saved.getCreatedById(), "Purchase order approved");

        // ✅ PUBLISH PO_APPROVED EVENT
        try {
            poEventPublisher.publishPOApproved(
                    saved.getPoId(),
                    saved.getSupplierId(),
                    saved.getWarehouseId(),
                    saved.getCreatedById(),
                    saved.getTotalAmount(),
                    saved.getExpectedDate());
        } catch (Exception e) {
            log.error("Failed to publish PO_APPROVED event: {}", e.getMessage());
        }

        return mapToDTO(saved);
    }

    @Transactional
    public PurchaseOrderResponseDTO rejectPO(Long id, String reason) {
        PurchaseOrder po = getPOEntity(id);
        if (po.getStatus() != POStatus.PENDING) {
            throw new InvalidPOStatusException(
                    "Only PENDING POs can be rejected. Current: " + po.getStatus());
        }
        POStatus previousStatus = po.getStatus();
        po.setStatus(POStatus.DRAFT);
        po.setNotes("REJECTED: " + reason
                + (po.getNotes() != null ? " | " + po.getNotes() : ""));
        PurchaseOrder saved = poRepository.save(po);
        audit(saved.getPoId(), previousStatus, saved.getStatus(),
                "REJECT_PO", saved.getCreatedById(), reason);
        return mapToDTO(saved);
    }

    @Transactional
    public PurchaseOrderResponseDTO cancelPO(Long id, String reason) {
        PurchaseOrder po = getPOEntity(id);
        if (po.getStatus() == POStatus.FULLY_RECEIVED
                || po.getStatus() == POStatus.CANCELLED) {
            throw new InvalidPOStatusException(
                    "Cannot cancel a " + po.getStatus() + " PO");
        }
        POStatus previousStatus = po.getStatus();
        po.setStatus(POStatus.CANCELLED);
        po.setNotes("CANCELLED: " + reason
                + (po.getNotes() != null ? " | " + po.getNotes() : ""));
        PurchaseOrder saved = poRepository.save(po);
        audit(saved.getPoId(), previousStatus, saved.getStatus(),
                "CANCEL_PO", saved.getCreatedById(), reason);
        return mapToDTO(saved);
    }

    @Transactional
    public PurchaseOrderResponseDTO receiveGoods(Long poId,
            List<GoodsReceiptDTO> receipts) {
        PurchaseOrder po = getPOEntity(poId);

        if (po.getStatus() != POStatus.APPROVED
                && po.getStatus() != POStatus.PARTIALLY_RECEIVED) {
            throw new InvalidPOStatusException(
                    "Goods can only be received for APPROVED or PARTIALLY_RECEIVED POs.");
        }

        for (GoodsReceiptDTO receipt : receipts) {
            POLineItem lineItem = po.getLineItems().stream()
                    .filter(i -> i.getLineItemId().equals(receipt.getLineItemId()))
                    .findFirst()
                    .orElseThrow(() -> new PurchaseOrderNotFoundException(
                            "Line item not found: " + receipt.getLineItemId()));

            int newQty = lineItem.getReceivedQty() + receipt.getReceivedQty();
            if (newQty > lineItem.getQuantity()) {
                throw new IllegalArgumentException(
                        "Received qty exceeds ordered qty for product "
                        + lineItem.getProductId());
            }
            lineItem.setReceivedQty(newQty);
            syncReceivedStock(po, lineItem, receipt.getReceivedQty());
        }

        boolean allReceived = po.getLineItems().stream()
                .allMatch(i -> i.getReceivedQty().equals(i.getQuantity()));

        POStatus previousStatus = po.getStatus();
        po.setStatus(allReceived
                ? POStatus.FULLY_RECEIVED : POStatus.PARTIALLY_RECEIVED);
        if (allReceived) po.setReceivedDate(LocalDate.now());

        PurchaseOrder saved = poRepository.save(po);
        audit(saved.getPoId(), previousStatus, saved.getStatus(),
                "RECEIVE_GOODS", saved.getCreatedById(), "Goods receipt recorded");
        return mapToDTO(saved);
    }

    private void syncReceivedStock(PurchaseOrder po, POLineItem lineItem,
            Integer receivedQty) {
        if (restTemplate == null || receivedQty == null || receivedQty <= 0) {
            return;
        }

        Long warehouseId = po.getWarehouseId();
        try {
            Map<String, Object> stockPayload = Map.of(
                    "productId", lineItem.getProductId(),
                    "quantity", receivedQty);

            @SuppressWarnings("unchecked")
            Map<String, Object> stock = restTemplate.postForObject(
                    warehouseUrl + "/stock/warehouse/" + warehouseId + "/receive",
                    stockPayload,
                    Map.class);

            Integer currentQuantity = extractInteger(stock, "quantity");
            if (currentQuantity == null) {
                currentQuantity = lineItem.getReceivedQty();
            }

            syncReportSnapshot(warehouseId, lineItem, currentQuantity);
            syncStockMovement(po, lineItem, receivedQty, currentQuantity);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to update warehouse stock for product "
                    + lineItem.getProductId() + ": " + e.getMessage(), e);
        }
    }

    private void syncReportSnapshot(Long warehouseId, POLineItem lineItem,
            Integer currentQuantity) {
        try {
            BigDecimal stockValue = lineItem.getUnitCost()
                    .multiply(BigDecimal.valueOf(currentQuantity));

            restTemplate.postForObject(
                    reportUrl + "/reports/snapshot"
                            + "?warehouseId=" + warehouseId
                            + "&productId=" + lineItem.getProductId()
                            + "&quantity=" + currentQuantity
                            + "&stockValue=" + stockValue,
                    null,
                    Map.class);
        } catch (Exception e) {
            log.error("Failed to sync report snapshot for product {}: {}",
                    lineItem.getProductId(), e.getMessage());
        }
    }

    private void syncStockMovement(PurchaseOrder po, POLineItem lineItem,
            Integer receivedQty, Integer currentQuantity) {
        try {
            Map<String, Object> movementPayload = Map.of(
                    "productId", lineItem.getProductId(),
                    "warehouseId", po.getWarehouseId(),
                    "movementType", "STOCK_IN",
                    "quantity", receivedQty,
                    "referenceId", po.getPoId(),
                    "referenceType", "PO",
                    "unitCost", lineItem.getUnitCost(),
                    "performedBy", po.getCreatedById(),
                    "notes", "Goods received for PO #" + po.getPoId(),
                    "balanceAfter", currentQuantity);

            restTemplate.postForObject(
                    movementUrl + "/movements",
                    movementPayload,
                    Map.class);
        } catch (Exception e) {
            log.error("Failed to sync stock movement for product {}: {}",
                    lineItem.getProductId(), e.getMessage());
        }
    }

    private Integer extractInteger(Map<String, Object> source, String key) {
        if (source == null || source.get(key) == null) {
            return null;
        }
        Object value = source.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(value.toString());
    }

    @Transactional
    public PurchaseOrderResponseDTO updatePO(Long id, PurchaseOrderRequestDTO dto) {
        PurchaseOrder po = getPOEntity(id);
        if (po.getStatus() != POStatus.DRAFT) {
            throw new InvalidPOStatusException(
                    "Only DRAFT POs can be updated. Current: " + po.getStatus());
        }
        po.setSupplierId(dto.getSupplierId());
        po.setWarehouseId(dto.getWarehouseId());
        po.setExpectedDate(dto.getExpectedDate());
        po.setNotes(dto.getNotes());
        po.setReferenceNumber(dto.getReferenceNumber());
        po.getLineItems().clear();
        List<POLineItem> newItems = dto.getLineItems().stream()
                .map(itemDto -> {
                    BigDecimal total = itemDto.getUnitCost()
                            .multiply(BigDecimal.valueOf(itemDto.getQuantity()));
                    return POLineItem.builder()
                            .productId(itemDto.getProductId())
                            .quantity(itemDto.getQuantity())
                            .unitCost(itemDto.getUnitCost())
                            .totalCost(total)
                            .receivedQty(0)
                            .purchaseOrder(po)
                            .build();
                }).toList();
        po.getLineItems().addAll(newItems);
        BigDecimal totalAmount = newItems.stream()
                .map(POLineItem::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        po.setTotalAmount(totalAmount);
        PurchaseOrder saved = poRepository.save(po);
        audit(saved.getPoId(), POStatus.DRAFT, saved.getStatus(),
                "UPDATE_PO", saved.getCreatedById(), "Draft purchase order updated");
        return mapToDTO(saved);
    }

    private void audit(Long poId, POStatus previousStatus, POStatus newStatus,
            String action, Long actorId, String details) {
        if (poAuditService != null) {
            poAuditService.record(poId, previousStatus, newStatus, action,
                    actorId, details);
        }
    }

    // ✅ SCHEDULED: Check for overdue POs every day at 9 AM
    @Scheduled(cron = "0 0 9 * * *")
    public void checkOverduePOs() {
        log.info("Running scheduled overdue PO check");
        List<PurchaseOrder> overduePOs = poRepository
                .findByStatusAndExpectedDateBefore(
                        POStatus.APPROVED, LocalDate.now());

        for (PurchaseOrder po : overduePOs) {
            try {
                poEventPublisher.publishPOOverdue(
                        po.getPoId(),
                        po.getSupplierId(),
                        po.getWarehouseId(),
                        po.getExpectedDate());
            } catch (Exception e) {
                log.error("Failed to publish PO_OVERDUE for PO {}: {}",
                        po.getPoId(), e.getMessage());
            }
        }

        log.info("Overdue PO check complete. Found {} overdue POs",
                overduePOs.size());
    }

    private PurchaseOrder getPOEntity(Long id) {
        return poRepository.findById(id)
                .orElseThrow(() -> new PurchaseOrderNotFoundException(
                        "Purchase order not found with ID: " + id));
    }

    private PurchaseOrderResponseDTO mapToDTO(PurchaseOrder po) {
        List<POLineItemResponseDTO> lineItems = po.getLineItems().stream()
                .map(item -> POLineItemResponseDTO.builder()
                        .lineItemId(item.getLineItemId())
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .unitCost(item.getUnitCost())
                        .totalCost(item.getTotalCost())
                        .receivedQty(item.getReceivedQty())
                        .build())
                .toList();

        return PurchaseOrderResponseDTO.builder()
                .poId(po.getPoId())
                .supplierId(po.getSupplierId())
                .warehouseId(po.getWarehouseId())
                .createdById(po.getCreatedById())
                .status(po.getStatus())
                .totalAmount(po.getTotalAmount())
                .orderDate(po.getOrderDate())
                .expectedDate(po.getExpectedDate())
                .receivedDate(po.getReceivedDate())
                .notes(po.getNotes())
                .referenceNumber(po.getReferenceNumber())
                .createdAt(po.getCreatedAt())
                .lineItems(lineItems)
                .build();
    }
}
