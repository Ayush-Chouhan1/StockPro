package com.stockpro.warehouseservice.repository;

import com.stockpro.warehouseservice.entity.StockAuditTrail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockAuditTrailRepository extends JpaRepository<StockAuditTrail, Long> {
}
