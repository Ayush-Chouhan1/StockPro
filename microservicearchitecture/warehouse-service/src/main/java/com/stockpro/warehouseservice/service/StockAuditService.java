package com.stockpro.warehouseservice.service;

import com.stockpro.warehouseservice.entity.StockAuditTrail;
import com.stockpro.warehouseservice.repository.StockAuditTrailRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class StockAuditService {

    @Autowired
    private StockAuditTrailRepository auditRepository;

    public void record(String action, Long productId, Long warehouseId,
            Long sourceWarehouseId, Long destinationWarehouseId,
            Integer quantity, String actor, String details) {
        try {
            auditRepository.save(StockAuditTrail.builder()
                    .action(action)
                    .productId(productId)
                    .warehouseId(warehouseId)
                    .sourceWarehouseId(sourceWarehouseId)
                    .destinationWarehouseId(destinationWarehouseId)
                    .quantity(quantity)
                    .actor(actor != null && !actor.isBlank() ? actor : "SYSTEM")
                    .details(details)
                    .build());
        } catch (Exception e) {
            log.error("Failed to write stock audit record for action {}: {}",
                    action, e.getMessage());
        }
    }
}
