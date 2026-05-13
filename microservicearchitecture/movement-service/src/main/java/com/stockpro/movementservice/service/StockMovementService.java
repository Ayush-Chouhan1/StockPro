package com.stockpro.movementservice.service;

import com.stockpro.movementservice.dto.*;
import com.stockpro.movementservice.entity.*;
import com.stockpro.movementservice.exception.MovementNotFoundException;
import com.stockpro.movementservice.repository.StockMovementRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class StockMovementService {

    @Autowired
    private StockMovementRepository movementRepository;

    @Autowired(required = false)
    private RestTemplate restTemplate;

    @Value("${services.warehouse-url:http://localhost:8084}")
    private String warehouseUrl;

    @Value("${services.report-url:http://localhost:8089}")
    private String reportUrl;

    // Movements are immutable — write-once only
    @Transactional
    public StockMovementResponseDTO recordMovement(
            StockMovementRequestDTO dto) {
        log.info("Recording movement: type={}, product={}, warehouse={}",
                dto.getMovementType(), dto.getProductId(), dto.getWarehouseId());

        StockMovement movement = StockMovement.builder()
                .productId(dto.getProductId())
                .warehouseId(dto.getWarehouseId())
                .movementType(dto.getMovementType())
                .quantity(dto.getQuantity())
                .referenceId(dto.getReferenceId())
                .referenceType(dto.getReferenceType())
                .unitCost(dto.getUnitCost())
                .performedBy(dto.getPerformedBy())
                .notes(dto.getNotes())
                .balanceAfter(dto.getBalanceAfter())
                .build();

        StockMovement saved = movementRepository.save(movement);
        syncStockLevel(dto);
        log.info("Movement recorded with ID: {}", saved.getMovementId());
        return mapToDTO(saved);
    }

    private void syncStockLevel(StockMovementRequestDTO dto) {
        if (restTemplate == null || dto.getBalanceAfter() == null) {
            return;
        }

        try {
            Map<String, Object> stockPayload = Map.of(
                    "productId", dto.getProductId(),
                    "quantity", dto.getBalanceAfter());

            restTemplate.put(
                    warehouseUrl + "/stock/warehouse/" + dto.getWarehouseId() + "/update",
                    stockPayload);

            syncReportSnapshot(dto);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to update warehouse stock for movement: "
                    + e.getMessage(), e);
        }
    }

    private void syncReportSnapshot(StockMovementRequestDTO dto) {
        try {
            BigDecimal unitCost = dto.getUnitCost() != null
                    ? dto.getUnitCost() : BigDecimal.ZERO;
            BigDecimal stockValue = unitCost
                    .multiply(BigDecimal.valueOf(dto.getBalanceAfter()));

            restTemplate.postForObject(
                    reportUrl + "/reports/snapshot"
                            + "?warehouseId=" + dto.getWarehouseId()
                            + "&productId=" + dto.getProductId()
                            + "&quantity=" + dto.getBalanceAfter()
                            + "&stockValue=" + stockValue,
                    null,
                    Map.class);
        } catch (Exception e) {
            log.error("Failed to sync report snapshot for movement: {}",
                    e.getMessage());
        }
    }

    public StockMovementResponseDTO getMovementById(Long id) {
        log.info("Fetching movement with ID: {}", id);
        StockMovement movement = movementRepository.findById(id)
                .orElseThrow(() -> new MovementNotFoundException(
                        "Movement not found with ID: " + id));
        return mapToDTO(movement);
    }

    public List<StockMovementResponseDTO> getAllMovements() {
        log.info("Fetching all movements");
        return movementRepository.findAll()
                .stream().map(this::mapToDTO).toList();
    }

    public List<StockMovementResponseDTO> getByProduct(Long productId) {
        return movementRepository.findByProductId(productId)
                .stream().map(this::mapToDTO).toList();
    }

    public List<StockMovementResponseDTO> getByWarehouse(Long warehouseId) {
        return movementRepository.findByWarehouseId(warehouseId)
                .stream().map(this::mapToDTO).toList();
    }

    public List<StockMovementResponseDTO> getByType(String type) {
        try {
            MovementType movementType = MovementType.valueOf(type.toUpperCase());
            return movementRepository.findByMovementType(movementType)
                    .stream().map(this::mapToDTO).toList();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid movement type: " + type);
        }
    }

    public List<StockMovementResponseDTO> getByReference(Long referenceId) {
        return movementRepository.findByReferenceId(referenceId)
                .stream().map(this::mapToDTO).toList();
    }

    public List<StockMovementResponseDTO> getByPerformedBy(Long userId) {
        return movementRepository.findByPerformedBy(userId)
                .stream().map(this::mapToDTO).toList();
    }

    public List<StockMovementResponseDTO> getByDateRange(
            LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException(
                    "Start date cannot be after end date");
        }
        return movementRepository.findByMovementDateBetween(start, end)
                .stream().map(this::mapToDTO).toList();
    }

    public List<StockMovementResponseDTO> getMovementHistory(
            Long productId, Long warehouseId) {
        log.info("Fetching movement history for product {} in warehouse {}",
                productId, warehouseId);
        return movementRepository
                .findByProductIdAndWarehouseIdOrderByMovementDateDesc(
                        productId, warehouseId)
                .stream().map(this::mapToDTO).toList();
    }

    public Integer getTotalStockIn(Long productId, Long warehouseId) {
        return movementRepository.getTotalStockIn(productId, warehouseId);
    }

    public Integer getTotalStockOut(Long productId, Long warehouseId) {
        return movementRepository.getTotalStockOut(productId, warehouseId);
    }

    private StockMovementResponseDTO mapToDTO(StockMovement m) {
        return StockMovementResponseDTO.builder()
                .movementId(m.getMovementId())
                .productId(m.getProductId())
                .warehouseId(m.getWarehouseId())
                .movementType(m.getMovementType())
                .quantity(m.getQuantity())
                .referenceId(m.getReferenceId())
                .referenceType(m.getReferenceType())
                .unitCost(m.getUnitCost())
                .performedBy(m.getPerformedBy())
                .notes(m.getNotes())
                .movementDate(m.getMovementDate())
                .balanceAfter(m.getBalanceAfter())
                .build();
    }
}
