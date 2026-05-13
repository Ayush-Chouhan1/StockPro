package com.stockpro.purchaseservice.repository;

import com.stockpro.purchaseservice.entity.POAuditTrail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface POAuditTrailRepository extends JpaRepository<POAuditTrail, Long> {
}
