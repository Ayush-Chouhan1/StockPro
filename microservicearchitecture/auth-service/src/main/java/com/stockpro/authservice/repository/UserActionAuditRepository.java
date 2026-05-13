package com.stockpro.authservice.repository;

import com.stockpro.authservice.entity.UserActionAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActionAuditRepository extends JpaRepository<UserActionAudit, Long> {
}
