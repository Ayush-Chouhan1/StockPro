package com.stockpro.purchaseservice.controller;

import com.stockpro.purchaseservice.dto.*;
import com.stockpro.purchaseservice.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/purchase-orders")
@Slf4j
@Tag(name = "Purchase Order Management",
     description = "APIs for managing purchase orders")
public class PurchaseOrderController {
 
    @Autowired
    private PurchaseOrderService poService;

    @PostMapping
    @Operation(summary = "Create a new purchase order")
    public ResponseEntity<PurchaseOrderResponseDTO> createPO(
            @Valid @RequestBody PurchaseOrderRequestDTO dto) {
        log.info("Request to create PO");
        return new ResponseEntity<>(poService.createPO(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get purchase order by ID")
    public ResponseEntity<PurchaseOrderResponseDTO> getPOById(
            @PathVariable Long id) {
        return ResponseEntity.ok(poService.getPOById(id));
    }

    @GetMapping
    @Operation(summary = "Get all purchase orders")
    public ResponseEntity<List<PurchaseOrderResponseDTO>> getAllPOs() {
        return ResponseEntity.ok(poService.getAllPOs());
    }

    @GetMapping("/supplier/{supplierId}")
    @Operation(summary = "Get POs by supplier")
    public ResponseEntity<List<PurchaseOrderResponseDTO>> getBySupplier(
            @PathVariable Long supplierId) {
        return ResponseEntity.ok(poService.getPOsBySupplier(supplierId));
    }

    @GetMapping("/warehouse/{warehouseId}")
    @Operation(summary = "Get POs by warehouse")
    public ResponseEntity<List<PurchaseOrderResponseDTO>> getByWarehouse(
            @PathVariable Long warehouseId) {
        return ResponseEntity.ok(poService.getPOsByWarehouse(warehouseId));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get POs by status")
    public ResponseEntity<List<PurchaseOrderResponseDTO>> getByStatus(
            @PathVariable String status) {
        return ResponseEntity.ok(poService.getPOsByStatus(status));
    }

    @GetMapping("/created-by/{userId}")
    @Operation(summary = "Get POs created by user")
    public ResponseEntity<List<PurchaseOrderResponseDTO>> getByCreatedBy(
            @PathVariable Long userId) {
        return ResponseEntity.ok(poService.getPOsByCreatedBy(userId));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get POs by date range")
    public ResponseEntity<List<PurchaseOrderResponseDTO>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {
        return ResponseEntity.ok(poService.getPOsByDateRange(startDate, endDate));
    }

    @GetMapping("/overdue")
    @Operation(summary = "Get overdue POs")
    public ResponseEntity<List<PurchaseOrderResponseDTO>> getOverduePOs() {
        return ResponseEntity.ok(poService.getOverduePOs());
    }

    @PutMapping("/{id}/submit")
    @Operation(summary = "Submit PO for approval")
    public ResponseEntity<PurchaseOrderResponseDTO> submitForApproval(
            @PathVariable Long id) {
        return ResponseEntity.ok(poService.submitForApproval(id));
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve a purchase order")
    public ResponseEntity<PurchaseOrderResponseDTO> approvePO(
            @PathVariable Long id) {
        return ResponseEntity.ok(poService.approvePO(id));
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "Reject a purchase order")
    public ResponseEntity<PurchaseOrderResponseDTO> rejectPO(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "No reason provided");
        return ResponseEntity.ok(poService.rejectPO(id, reason));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel a purchase order")
    public ResponseEntity<PurchaseOrderResponseDTO> cancelPO(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "No reason provided");
        return ResponseEntity.ok(poService.cancelPO(id, reason));
    }

    @PostMapping("/{id}/receive-goods")
    @Operation(summary = "Record goods receipt against a PO")
    public ResponseEntity<PurchaseOrderResponseDTO> receiveGoods(
            @PathVariable Long id,
            @Valid @RequestBody List<GoodsReceiptDTO> receipts) {
        return ResponseEntity.ok(poService.receiveGoods(id, receipts));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a DRAFT purchase order")
    public ResponseEntity<PurchaseOrderResponseDTO> updatePO(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseOrderRequestDTO dto) {
        return ResponseEntity.ok(poService.updatePO(id, dto));
    }
}