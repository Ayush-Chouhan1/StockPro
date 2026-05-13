package com.stockpro.movementservice.controller;

import com.stockpro.movementservice.dto.*;
import com.stockpro.movementservice.service.StockMovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/movements")
@Slf4j
@Tag(name = "Stock Movement",
     description = "APIs for recording and querying stock movements")
public class StockMovementController {

    @Autowired
    private StockMovementService movementService;

    @PostMapping
    @Operation(summary = "Record a new stock movement (immutable)")
    public ResponseEntity<StockMovementResponseDTO> recordMovement(
            @Valid @RequestBody StockMovementRequestDTO dto) {
        log.info("Recording movement: {}", dto.getMovementType());
        return new ResponseEntity<>(
                movementService.recordMovement(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get movement by ID")
    public ResponseEntity<StockMovementResponseDTO> getById(
            @PathVariable Long id) {
        return ResponseEntity.ok(movementService.getMovementById(id));
    }

    @GetMapping
    @Operation(summary = "Get all movements")
    public ResponseEntity<List<StockMovementResponseDTO>> getAll() {
        return ResponseEntity.ok(movementService.getAllMovements());
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get movements by product")
    public ResponseEntity<List<StockMovementResponseDTO>> getByProduct(
            @PathVariable Long productId) {
        return ResponseEntity.ok(movementService.getByProduct(productId));
    }

    @GetMapping("/warehouse/{warehouseId}")
    @Operation(summary = "Get movements by warehouse")
    public ResponseEntity<List<StockMovementResponseDTO>> getByWarehouse(
            @PathVariable Long warehouseId) {
        return ResponseEntity.ok(movementService.getByWarehouse(warehouseId));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get movements by type")
    public ResponseEntity<List<StockMovementResponseDTO>> getByType(
            @PathVariable String type) {
        return ResponseEntity.ok(movementService.getByType(type));
    }

    @GetMapping("/reference/{referenceId}")
    @Operation(summary = "Get movements by reference ID (PO/issue order)")
    public ResponseEntity<List<StockMovementResponseDTO>> getByReference(
            @PathVariable Long referenceId) {
        return ResponseEntity.ok(movementService.getByReference(referenceId));
    }

    @GetMapping("/performed-by/{userId}")
    @Operation(summary = "Get movements by user who performed them")
    public ResponseEntity<List<StockMovementResponseDTO>> getByPerformedBy(
            @PathVariable Long userId) {
        return ResponseEntity.ok(movementService.getByPerformedBy(userId));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get movements by date range")
    public ResponseEntity<List<StockMovementResponseDTO>> getByDateRange(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime start,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime end) {
        return ResponseEntity.ok(movementService.getByDateRange(start, end));
    }

    @GetMapping("/history/product/{productId}/warehouse/{warehouseId}")
    @Operation(summary = "Get movement history for a product in a warehouse")
    public ResponseEntity<List<StockMovementResponseDTO>> getHistory(
            @PathVariable Long productId,
            @PathVariable Long warehouseId) {
        return ResponseEntity.ok(
                movementService.getMovementHistory(productId, warehouseId));
    }

    @GetMapping("/stock-in/product/{productId}/warehouse/{warehouseId}")
    @Operation(summary = "Get total stock-in for a product in a warehouse")
    public ResponseEntity<Map<String, Integer>> getTotalStockIn(
            @PathVariable Long productId,
            @PathVariable Long warehouseId) {
        return ResponseEntity.ok(Map.of("totalStockIn",
                movementService.getTotalStockIn(productId, warehouseId)));
    }

    @GetMapping("/stock-out/product/{productId}/warehouse/{warehouseId}")
    @Operation(summary = "Get total stock-out for a product in a warehouse")
    public ResponseEntity<Map<String, Integer>> getTotalStockOut(
            @PathVariable Long productId,
            @PathVariable Long warehouseId) {
        return ResponseEntity.ok(Map.of("totalStockOut",
                movementService.getTotalStockOut(productId, warehouseId)));
    }
}