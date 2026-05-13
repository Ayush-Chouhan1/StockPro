package com.stockpro.reportservice.controller;

import com.stockpro.reportservice.dto.*;
import com.stockpro.reportservice.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reports")
@Slf4j
@Tag(name = "Reports & Analytics",
     description = "APIs for inventory reports and analytics")
public class ReportController {

    @Autowired
    private ReportService reportService;

    // ==================== SNAPSHOT ====================

    @PostMapping("/snapshot")
    @Operation(summary = "Take a manual inventory snapshot")
    public ResponseEntity<InventorySnapshotDTO> takeSnapshot(
            @RequestParam Long warehouseId,
            @RequestParam Long productId,
            @RequestParam Integer quantity,
            @RequestParam BigDecimal stockValue) {
        return ResponseEntity.ok(
                reportService.takeSnapshot(
                        warehouseId, productId, quantity, stockValue));
    }

    @GetMapping("/snapshot/date/{date}")
    @Operation(summary = "Get snapshots for a specific date")
    public ResponseEntity<List<InventorySnapshotDTO>> getSnapshotsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        return ResponseEntity.ok(reportService.getSnapshotsByDate(date));
    }

    @GetMapping("/snapshot/warehouse/{warehouseId}")
    @Operation(summary = "Get snapshots for a warehouse")
    public ResponseEntity<List<InventorySnapshotDTO>> getSnapshotsByWarehouse(
            @PathVariable Long warehouseId) {
        return ResponseEntity.ok(
                reportService.getSnapshotsByWarehouse(warehouseId));
    }

    @GetMapping("/snapshot/date-range")
    @Operation(summary = "Get snapshots for a date range")
    public ResponseEntity<List<InventorySnapshotDTO>> getSnapshotsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {
        return ResponseEntity.ok(
                reportService.getSnapshotsByDateRange(startDate, endDate));
    }

    @GetMapping("/snapshot/latest")
    @Operation(summary = "Get latest snapshot")
    public ResponseEntity<List<InventorySnapshotDTO>> getLatestSnapshot() {
        return ResponseEntity.ok(reportService.getLatestSnapshot());
    }

    // ==================== VALUATION ====================

    @GetMapping("/valuation/total")
    @Operation(summary = "Get total inventory valuation")
    public ResponseEntity<StockValuationDTO> getTotalStockValue() {
        return ResponseEntity.ok(reportService.getTotalStockValue());
    }

    @GetMapping("/valuation/warehouse/{warehouseId}")
    @Operation(summary = "Get inventory valuation by warehouse")
    public ResponseEntity<StockValuationDTO> getStockValueByWarehouse(
            @PathVariable Long warehouseId) {
        return ResponseEntity.ok(
                reportService.getStockValueByWarehouse(warehouseId));
    }

    // ==================== TURNOVER ====================

    @GetMapping("/turnover")
    @Operation(summary = "Get inventory turnover rate")
    public ResponseEntity<Map<String, Object>> getInventoryTurnover(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {
        return ResponseEntity.ok(
                reportService.getInventoryTurnover(startDate, endDate));
    }

    // ==================== LOW STOCK ====================

    @GetMapping("/low-stock")
    @Operation(summary = "Get low stock report")
    public ResponseEntity<List<InventorySnapshotDTO>> getLowStockReport(
            @RequestParam(defaultValue = "10") Integer threshold) {
        return ResponseEntity.ok(reportService.getLowStockReport(threshold));
    }

    // ==================== DEAD STOCK ====================

    @GetMapping("/dead-stock")
    @Operation(summary = "Get dead stock (no movement for N days)")
    public ResponseEntity<List<DeadStockDTO>> getDeadStock(
            @RequestParam(required = false) Integer days) {
        return ResponseEntity.ok(reportService.getDeadStock(days));
    }

    // ==================== TOP / SLOW MOVERS ====================

    @GetMapping("/top-moving")
    @Operation(summary = "Get top moving products")
    public ResponseEntity<List<TopMovingProductDTO>> getTopMovingProducts(
            @RequestParam(defaultValue = "10") Integer limit) {
        return ResponseEntity.ok(reportService.getTopMovingProducts(limit));
    }

    @GetMapping("/slow-moving")
    @Operation(summary = "Get slow moving products")
    public ResponseEntity<List<TopMovingProductDTO>> getSlowMovingProducts(
            @RequestParam(required = false) Integer days) {
        return ResponseEntity.ok(reportService.getSlowMovingProducts(days));
    }

    // ==================== PO SUMMARY ====================

    @GetMapping("/po-summary")
    @Operation(summary = "Get purchase order summary")
    public ResponseEntity<POSummaryDTO> getPOSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {
        return ResponseEntity.ok(
                reportService.getPOSummary(startDate, endDate));
    }

    // ==================== MOVEMENT SUMMARY ====================

    @GetMapping("/movement-summary")
    @Operation(summary = "Get stock movement summary")
    public ResponseEntity<Map<String, Object>> getMovementSummary(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {
        return ResponseEntity.ok(
                reportService.getStockMovementSummary(
                        warehouseId, startDate, endDate));
    }
}