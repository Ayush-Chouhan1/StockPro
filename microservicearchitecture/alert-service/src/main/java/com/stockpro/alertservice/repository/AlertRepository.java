package com.stockpro.alertservice.repository;

import com.stockpro.alertservice.entity.Alert;
import com.stockpro.alertservice.entity.AlertType;
import com.stockpro.alertservice.entity.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByRecipientId(Long recipientId);

    List<Alert> findByRecipientIdAndIsRead(Long recipientId, Boolean isRead);

    List<Alert> findByRecipientIdAndIsAcknowledged(Long recipientId, Boolean isAcknowledged);

    List<Alert> findByType(AlertType type);

    List<Alert> findBySeverity(Severity severity);

    List<Alert> findByRelatedProductId(Long productId);

    List<Alert> findByRelatedWarehouseId(Long warehouseId);

    Long countByRecipientIdAndIsRead(Long recipientId, Boolean isRead);

    @Query("SELECT a FROM Alert a WHERE a.recipientId = :recipientId AND a.isRead = false ORDER BY a.createdAt DESC")
    List<Alert> findUnreadAlertsByRecipient(@Param("recipientId") Long recipientId);

    @Query("SELECT a FROM Alert a WHERE a.recipientId = :recipientId AND a.isAcknowledged = false AND a.severity = 'CRITICAL' ORDER BY a.createdAt DESC")
    List<Alert> findUnacknowledgedCriticalAlerts(@Param("recipientId") Long recipientId);

    @Query("SELECT a FROM Alert a WHERE a.createdAt >= :startDate ORDER BY a.createdAt DESC")
    List<Alert> findRecentAlerts(@Param("startDate") LocalDateTime startDate);
}