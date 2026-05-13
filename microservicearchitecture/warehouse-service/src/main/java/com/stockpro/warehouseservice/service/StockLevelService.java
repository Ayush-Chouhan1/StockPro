package com.stockpro.warehouseservice.service;

import com.stockpro.warehouseservice.dto.*;
import com.stockpro.warehouseservice.entity.StockLevel;
import com.stockpro.warehouseservice.exception.*;
import com.stockpro.warehouseservice.rabbitmq.StockEventPublisher;
import com.stockpro.warehouseservice.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class StockLevelService {

    @Autowired
    private StockLevelRepository stockLevelRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private StockEventPublisher stockEventPublisher;

    @Autowired
    private StockAuditService stockAuditService;

    public StockLevelResponseDTO getStockLevel(Long warehouseId, Long productId) {
        log.info("Fetching stock for product {} in warehouse {}", productId, warehouseId);
        StockLevel stock = stockLevelRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new WarehouseNotFoundException(
                        "Stock not found for product " + productId
                        + " in warehouse " + warehouseId));
        return mapToDTO(stock);
    }

    public List<StockLevelResponseDTO> getStockByWarehouse(Long warehouseId) {
        log.info("Fetching all stock for warehouse: {}", warehouseId);
        validateWarehouseExists(warehouseId);
        return stockLevelRepository.findByWarehouseId(warehouseId)
                .stream().map(this::mapToDTO).toList();
    }

    public List<StockLevelResponseDTO> getStockByProduct(Long productId) {
        log.info("Fetching stock across all warehouses for product: {}", productId);
        return stockLevelRepository.findByProductId(productId)
                .stream().map(this::mapToDTO).toList();
    }

    @Transactional
    public StockLevelResponseDTO updateStock(Long warehouseId, StockUpdateDTO dto) {
        log.info("Updating stock for product {} in warehouse {}",
                dto.getProductId(), warehouseId);
        validateWarehouseExists(warehouseId);

        StockLevel stock = stockLevelRepository
                .findByWarehouseIdAndProductId(warehouseId, dto.getProductId())
                .orElseGet(() -> StockLevel.builder()
                        .warehouseId(warehouseId)
                        .productId(dto.getProductId())
                        .quantity(0)
                        .reservedQuantity(0)
                        .build());

        stock.setQuantity(dto.getQuantity());
        if (dto.getBinLocation() != null) {
            stock.setBinLocation(dto.getBinLocation());
        }

		StockLevel saved = stockLevelRepository.save(stock);
		recalculateWarehouseUsage(warehouseId);

        // ✅ CHECK AND PUBLISH STOCK LEVEL EVENTS
        checkAndPublishStockEvents(saved, dto.getReorderLevel(), dto.getMaxStockLevel());
        audit("STOCK_UPDATE", dto.getProductId(), warehouseId,
                null, null, dto.getQuantity(), null,
                "Stock level set to " + dto.getQuantity());

        log.info("Stock updated successfully");
        return mapToDTO(saved);
    }

    @Transactional
    public StockLevelResponseDTO receiveStock(Long warehouseId, StockReceiveDTO dto) {
        log.info("Receiving {} units of product {} in warehouse {}",
                dto.getQuantity(), dto.getProductId(), warehouseId);
        validateWarehouseExists(warehouseId);

        StockLevel stock = stockLevelRepository
                .findByWarehouseIdAndProductId(warehouseId, dto.getProductId())
                .orElseGet(() -> StockLevel.builder()
                        .warehouseId(warehouseId)
                        .productId(dto.getProductId())
                        .quantity(0)
                        .reservedQuantity(0)
                        .build());

        stock.setQuantity(stock.getQuantity() + dto.getQuantity());
        if (dto.getBinLocation() != null) {
            stock.setBinLocation(dto.getBinLocation());
        }

		StockLevel saved = stockLevelRepository.save(stock);
		recalculateWarehouseUsage(warehouseId);
        audit("STOCK_RECEIVE", dto.getProductId(), warehouseId,
                null, null, dto.getQuantity(), null,
                "Received stock. Balance after: " + saved.getQuantity());
        log.info("Received stock successfully. New quantity: {}", saved.getQuantity());
        return mapToDTO(saved);
    }

    @Transactional
    public void reserveStock(Long warehouseId, Long productId, Integer quantity) {
        log.info("Reserving {} units of product {} in warehouse {}",
                quantity, productId, warehouseId);

        StockLevel stock = stockLevelRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new WarehouseNotFoundException(
                        "Stock not found for product " + productId));

        if (stock.getAvailableQuantity() < quantity) {
            throw new InsufficientStockException(
                    "Insufficient stock. Available: "
                    + stock.getAvailableQuantity()
                    + ", Requested: " + quantity);
        }

        stock.setReservedQuantity(stock.getReservedQuantity() + quantity);
        stockLevelRepository.save(stock);
        audit("STOCK_RESERVE", productId, warehouseId,
                null, null, quantity, null, "Reserved stock");
        log.info("Stock reserved successfully");
    }

    @Transactional
    public void releaseReservation(Long warehouseId, Long productId, Integer quantity) {
        log.info("Releasing reservation of {} units of product {} in warehouse {}",
                quantity, productId, warehouseId);

        StockLevel stock = stockLevelRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new WarehouseNotFoundException(
                        "Stock not found for product " + productId));

        int newReserved = stock.getReservedQuantity() - quantity;
        stock.setReservedQuantity(Math.max(0, newReserved));
        stockLevelRepository.save(stock);
        audit("STOCK_RELEASE_RESERVATION", productId, warehouseId,
                null, null, quantity, null, "Released stock reservation");
    }

    @Transactional
    public void transferStock(StockTransferDTO dto) {
        log.info("Transferring {} units of product {} from warehouse {} to {}",
                dto.getQuantity(), dto.getProductId(),
                dto.getFromWarehouseId(), dto.getToWarehouseId());

        if (dto.getFromWarehouseId().equals(dto.getToWarehouseId())) {
            throw new IllegalArgumentException(
                    "Source and destination warehouses cannot be the same");
        }

        validateWarehouseExists(dto.getFromWarehouseId());
        validateWarehouseExists(dto.getToWarehouseId());

        StockLevel source = stockLevelRepository
                .findByWarehouseIdAndProductId(
                        dto.getFromWarehouseId(), dto.getProductId())
                .orElseThrow(() -> new InsufficientStockException(
                        "No stock found in source warehouse for product "
                        + dto.getProductId()));

        if (source.getAvailableQuantity() < dto.getQuantity()) {
            throw new InsufficientStockException(
                    "Insufficient stock in source warehouse. Available: "
                    + source.getAvailableQuantity()
                    + ", Requested: " + dto.getQuantity());
        }

		source.setQuantity(source.getQuantity() - dto.getQuantity());
		stockLevelRepository.save(source);
		recalculateWarehouseUsage(dto.getFromWarehouseId());

        StockLevel destination = stockLevelRepository
                .findByWarehouseIdAndProductId(
                        dto.getToWarehouseId(), dto.getProductId())
                .orElseGet(() -> StockLevel.builder()
                        .warehouseId(dto.getToWarehouseId())
                        .productId(dto.getProductId())
                        .quantity(0)
                        .reservedQuantity(0)
                        .build());

		destination.setQuantity(destination.getQuantity() + dto.getQuantity());
		stockLevelRepository.save(destination);
		recalculateWarehouseUsage(dto.getToWarehouseId());
        audit("INTER_WAREHOUSE_TRANSFER", dto.getProductId(), null,
                dto.getFromWarehouseId(), dto.getToWarehouseId(), dto.getQuantity(),
                null, dto.getReason());

        log.info("Stock transfer completed successfully");
    }

    public List<StockLevelResponseDTO> getLowStockItems(Integer threshold) {
        log.info("Fetching low stock items below threshold: {}", threshold);
        return stockLevelRepository.findLowStockItems(threshold)
                .stream().map(this::mapToDTO).toList();
    }

    // ✅ NEW: Check thresholds and publish events
    private void checkAndPublishStockEvents(StockLevel stock,
            Integer reorderLevel, Integer maxStockLevel) {
        try {
            if (reorderLevel != null && stock.getQuantity() <= reorderLevel) {
                log.warn("LOW STOCK detected! Product: {}, Warehouse: {}, Qty: {}",
                        stock.getProductId(), stock.getWarehouseId(),
                        stock.getQuantity());
                stockEventPublisher.publishLowStockEvent(
                        stock.getProductId(),
                        stock.getWarehouseId(),
                        stock.getQuantity(),
                        reorderLevel);
            }

            if (maxStockLevel != null && stock.getQuantity() > maxStockLevel) {
                log.warn("OVERSTOCK detected! Product: {}, Warehouse: {}, Qty: {}",
                        stock.getProductId(), stock.getWarehouseId(),
                        stock.getQuantity());
                stockEventPublisher.publishOverstockEvent(
                        stock.getProductId(),
                        stock.getWarehouseId(),
                        stock.getQuantity(),
                        maxStockLevel);
            }
        } catch (Exception e) {
            // Don't fail stock update if RabbitMQ is down
            log.error("Failed to publish stock event: {}", e.getMessage());
        }
    }

	private void validateWarehouseExists(Long warehouseId) {
		if (!warehouseRepository.existsById(warehouseId)) {
			throw new WarehouseNotFoundException(
					"Warehouse not found with ID: " + warehouseId);
		}
	}

	private void recalculateWarehouseUsage(Long warehouseId) {
		warehouseRepository.findById(warehouseId).ifPresent(warehouse -> {
			Long totalQuantity = stockLevelRepository.sumQuantityByWarehouseId(warehouseId);
			int usedCapacity = totalQuantity != null ? totalQuantity.intValue() : 0;
			warehouse.setUsedCapacity(usedCapacity);
			warehouseRepository.save(warehouse);
		});
	}

    private void audit(String action, Long productId, Long warehouseId,
            Long sourceWarehouseId, Long destinationWarehouseId,
            Integer quantity, String actor, String details) {
        if (stockAuditService != null) {
            stockAuditService.record(action, productId, warehouseId,
                    sourceWarehouseId, destinationWarehouseId, quantity,
                    actor, details);
        }
    }

    private StockLevelResponseDTO mapToDTO(StockLevel s) {
        return StockLevelResponseDTO.builder()
                .stockId(s.getStockId())
                .warehouseId(s.getWarehouseId())
                .productId(s.getProductId())
                .quantity(s.getQuantity())
                .reservedQuantity(s.getReservedQuantity())
                .availableQuantity(s.getAvailableQuantity())
                .binLocation(s.getBinLocation())
                .lastUpdated(s.getLastUpdated())
                .build();
    }
}
