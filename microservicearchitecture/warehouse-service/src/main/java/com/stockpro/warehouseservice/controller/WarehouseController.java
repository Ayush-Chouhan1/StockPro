package com.stockpro.warehouseservice.controller;

import com.stockpro.warehouseservice.dto.*;
import com.stockpro.warehouseservice.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/warehouses")
@Slf4j
@Tag(name = "Warehouse Management", description = "APIs for managing warehouses")
public class WarehouseController {

    @Autowired
    private WarehouseService warehouseService;

    @PostMapping
    @Operation(summary = "Create a new warehouse")
    public ResponseEntity<WarehouseResponseDTO> createWarehouse(
            @Valid @RequestBody WarehouseRequestDTO dto) {
        log.info("Request to create warehouse: {}", dto.getName());
        return new ResponseEntity<>(
                warehouseService.createWarehouse(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get warehouse by ID")
    public ResponseEntity<WarehouseResponseDTO> getWarehouseById(
            @PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.getWarehouseById(id));
    }

    @GetMapping
    @Operation(summary = "Get all warehouses")
    public ResponseEntity<List<WarehouseResponseDTO>> getAllWarehouses() {
        return ResponseEntity.ok(warehouseService.getAllWarehouses());
    }

    @GetMapping("/active")
    @Operation(summary = "Get all active warehouses")
    public ResponseEntity<List<WarehouseResponseDTO>> getActiveWarehouses() {
        return ResponseEntity.ok(warehouseService.getActiveWarehouses());
    }

    @GetMapping("/manager/{managerId}")
    @Operation(summary = "Get warehouses by manager ID")
    public ResponseEntity<List<WarehouseResponseDTO>> getByManager(
            @PathVariable Long managerId) {
        return ResponseEntity.ok(
                warehouseService.getWarehousesByManager(managerId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update warehouse")
    public ResponseEntity<WarehouseResponseDTO> updateWarehouse(
            @PathVariable Long id,
            @Valid @RequestBody WarehouseRequestDTO dto) {
        return ResponseEntity.ok(warehouseService.updateWarehouse(id, dto));
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate warehouse")
    public ResponseEntity<String> deactivateWarehouse(@PathVariable Long id) {
        warehouseService.deactivateWarehouse(id);
        return ResponseEntity.ok("Warehouse deactivated successfully");
    }

    @PutMapping("/{warehouseId}/manager/{managerId}")
    @Operation(summary = "Assign manager to warehouse")
    public ResponseEntity<String> assignManager(
            @PathVariable Long warehouseId,
            @PathVariable Long managerId) {
        warehouseService.assignManager(warehouseId, managerId);
        return ResponseEntity.ok("Manager assigned successfully");
    }
}