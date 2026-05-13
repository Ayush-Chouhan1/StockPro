package com.stockpro.warehouseservice.controller;

import com.stockpro.warehouseservice.dto.*;
import com.stockpro.warehouseservice.service.StockLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stock")
@Slf4j
@Tag(name = "Stock Level Management", description = "APIs for managing stock levels")
public class StockLevelController {

    @Autowired
    private StockLevelService stockLevelService;

    @GetMapping("/warehouse/{warehouseId}/product/{productId}")
    @Operation(summary = "Get stock level for a product in a warehouse")
    public ResponseEntity<StockLevelResponseDTO> getStockLevel(
            @PathVariable Long warehouseId,
            @PathVariable Long productId) {
        return ResponseEntity.ok(
                stockLevelService.getStockLevel(warehouseId, productId));
    }

    @GetMapping("/warehouse/{warehouseId}")
    @Operation(summary = "Get all stock levels for a warehouse")
    public ResponseEntity<List<StockLevelResponseDTO>> getStockByWarehouse(
            @PathVariable Long warehouseId) {
        return ResponseEntity.ok(
                stockLevelService.getStockByWarehouse(warehouseId));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get stock levels across all warehouses for a product")
    public ResponseEntity<List<StockLevelResponseDTO>> getStockByProduct(
            @PathVariable Long productId) {
        return ResponseEntity.ok(
                stockLevelService.getStockByProduct(productId));
    }

    @PutMapping("/warehouse/{warehouseId}/update")
    @Operation(summary = "Update stock level for a product in a warehouse")
    public ResponseEntity<StockLevelResponseDTO> updateStock(
            @PathVariable Long warehouseId,
            @Valid @RequestBody StockUpdateDTO dto) {
        return ResponseEntity.ok(
                stockLevelService.updateStock(warehouseId, dto));
    }

    @PostMapping("/warehouse/{warehouseId}/receive")
    @Operation(summary = "Add received stock to a product in a warehouse")
    public ResponseEntity<StockLevelResponseDTO> receiveStock(
            @PathVariable Long warehouseId,
            @Valid @RequestBody StockReceiveDTO dto) {
        return ResponseEntity.ok(
                stockLevelService.receiveStock(warehouseId, dto));
    }

    @PostMapping("/reserve")
    @Operation(summary = "Reserve stock")
    public ResponseEntity<String> reserveStock(
            @RequestParam Long warehouseId,
            @RequestParam Long productId,
            @RequestParam Integer quantity) {
        stockLevelService.reserveStock(warehouseId, productId, quantity);
        return ResponseEntity.ok("Stock reserved successfully");
    }

    @PostMapping("/release")
    @Operation(summary = "Release reserved stock")
    public ResponseEntity<String> releaseReservation(
            @RequestParam Long warehouseId,
            @RequestParam Long productId,
            @RequestParam Integer quantity) {
        stockLevelService.releaseReservation(warehouseId, productId, quantity);
        return ResponseEntity.ok("Reservation released successfully");
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer stock between warehouses")
    public ResponseEntity<String> transferStock(
            @Valid @RequestBody StockTransferDTO dto) {
        stockLevelService.transferStock(dto);
        return ResponseEntity.ok("Stock transferred successfully");
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get low stock items")
    public ResponseEntity<List<StockLevelResponseDTO>> getLowStockItems(
            @RequestParam(defaultValue = "10") Integer threshold) {
        return ResponseEntity.ok(
                stockLevelService.getLowStockItems(threshold));
    }
}
