package com.stockpro.supplierservice.controller;

import com.stockpro.supplierservice.dto.SupplierRequestDTO;
import com.stockpro.supplierservice.dto.SupplierResponseDTO;
import com.stockpro.supplierservice.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/suppliers")
@Slf4j
@Tag(name = "Supplier Management", description = "APIs for managing suppliers/vendors")
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    @PostMapping
    @Operation(summary = "Create new supplier")
    public ResponseEntity<SupplierResponseDTO> createSupplier(@Valid @RequestBody SupplierRequestDTO dto) {
        log.info("Received request to create supplier");
        SupplierResponseDTO response = supplierService.createSupplier(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get supplier by ID")
    public ResponseEntity<SupplierResponseDTO> getSupplierById(@PathVariable Long id) {
        log.info("Received request to get supplier with ID: {}", id);
        SupplierResponseDTO response = supplierService.getSupplierById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all suppliers")
    public ResponseEntity<List<SupplierResponseDTO>> getAllSuppliers() {
        log.info("Received request to get all suppliers");
        List<SupplierResponseDTO> response = supplierService.getAllSuppliers();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    @Operation(summary = "Get all active suppliers")
    public ResponseEntity<List<SupplierResponseDTO>> getActiveSuppliers() {
        log.info("Received request to get active suppliers");
        List<SupplierResponseDTO> response = supplierService.getActiveSuppliers();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/city/{city}")
    @Operation(summary = "Get suppliers by city")
    public ResponseEntity<List<SupplierResponseDTO>> getSuppliersByCity(@PathVariable String city) {
        log.info("Received request to get suppliers by city: {}", city);
        List<SupplierResponseDTO> response = supplierService.getSuppliersByCity(city);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/country/{country}")
    @Operation(summary = "Get suppliers by country")
    public ResponseEntity<List<SupplierResponseDTO>> getSuppliersByCountry(@PathVariable String country) {
        log.info("Received request to get suppliers by country: {}", country);
        List<SupplierResponseDTO> response = supplierService.getSuppliersByCountry(country);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(summary = "Search suppliers")
    public ResponseEntity<List<SupplierResponseDTO>> searchSuppliers(@RequestParam String keyword) {
        log.info("Received request to search suppliers with keyword: {}", keyword);
        List<SupplierResponseDTO> response = supplierService.searchSuppliers(keyword);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/top-rated")
    @Operation(summary = "Get top-rated suppliers")
    public ResponseEntity<List<SupplierResponseDTO>> getTopRatedSuppliers(
            @RequestParam(defaultValue = "4.0") Double minRating) {
        log.info("Received request to get top-rated suppliers with min rating: {}", minRating);
        List<SupplierResponseDTO> response = supplierService.getTopRatedSuppliers(minRating);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update supplier")
    public ResponseEntity<SupplierResponseDTO> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequestDTO dto) {
        log.info("Received request to update supplier with ID: {}", id);
        SupplierResponseDTO response = supplierService.updateSupplier(id, dto);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/rating")
    @Operation(summary = "Update supplier rating")
    public ResponseEntity<String> updateRating(
            @PathVariable Long id,
            @RequestParam Double rating) {
        log.info("Received request to update rating for supplier ID: {}", id);
        supplierService.updateRating(id, rating);
        return ResponseEntity.ok("Supplier rating updated successfully");
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate supplier")
    public ResponseEntity<String> deactivateSupplier(@PathVariable Long id) {
        log.info("Received request to deactivate supplier with ID: {}", id);
        supplierService.deactivateSupplier(id);
        return ResponseEntity.ok("Supplier deactivated successfully");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete supplier")
    public ResponseEntity<String> deleteSupplier(@PathVariable Long id) {
        log.info("Received request to delete supplier with ID: {}", id);
        supplierService.deleteSupplier(id);
        return ResponseEntity.ok("Supplier deleted successfully");
    }
}