package com.stockpro.purchaseservice.service;

import com.stockpro.purchaseservice.entity.POAuditTrail;
import com.stockpro.purchaseservice.entity.POStatus;
import com.stockpro.purchaseservice.repository.POAuditTrailRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class POAuditService {

    @Autowired
    private POAuditTrailRepository auditRepository;

    public void record(Long poId, POStatus previousStatus, POStatus newStatus,
            String action, Long actorId, String details) {
        try {
            auditRepository.save(POAuditTrail.builder()
                    .poId(poId)
                    .previousStatus(previousStatus)
                    .newStatus(newStatus)
                    .action(action)
                    .actorId(actorId)
                    .details(details)
                    .build());
        } catch (Exception e) {
            log.error("Failed to write PO audit record for PO {}: {}",
                    poId, e.getMessage());
        }
    }
}
