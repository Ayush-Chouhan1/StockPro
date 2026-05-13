package com.stockpro.authservice.service;

import com.stockpro.authservice.entity.UserActionAudit;
import com.stockpro.authservice.repository.UserActionAuditRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuditService {

    @Autowired
    private UserActionAuditRepository auditRepository;

    public void record(String action, String actorEmail, String targetEmail,
            String targetType, String targetId, String details) {
        try {
            auditRepository.save(UserActionAudit.builder()
                    .action(action)
                    .actorEmail(actorEmail != null ? actorEmail : "SYSTEM")
                    .targetEmail(targetEmail)
                    .targetType(targetType)
                    .targetId(targetId)
                    .details(details)
                    .build());
        } catch (Exception e) {
            log.error("Failed to write auth audit record for action {}: {}",
                    action, e.getMessage());
        }
    }
}
