package com.stockpro.alertservice.controller;

import com.stockpro.alertservice.dto.AlertRequestDTO;
import com.stockpro.alertservice.dto.AlertResponseDTO;
import com.stockpro.alertservice.service.AlertService;
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
@RequestMapping("/alerts")
@Slf4j
@Tag(name = "Alert Management", description = "APIs for managing alerts and notifications")
public class AlertController {

    @Autowired
    private AlertService alertService;

    @PostMapping
    @Operation(summary = "Create new alert")
    public ResponseEntity<AlertResponseDTO> createAlert(@Valid @RequestBody AlertRequestDTO dto) {
        log.info("Received request to create alert");
        AlertResponseDTO response = alertService.createAlert(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get alert by ID")
    public ResponseEntity<AlertResponseDTO> getAlertById(@PathVariable Long id) {
        log.info("Received request to get alert with ID: {}", id);
        AlertResponseDTO response = alertService.getAlertById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recipient/{recipientId}")
    @Operation(summary = "Get all alerts for a recipient")
    public ResponseEntity<List<AlertResponseDTO>> getAlertsByRecipient(@PathVariable Long recipientId) {
        log.info("Received request to get alerts for recipient: {}", recipientId);
        List<AlertResponseDTO> response = alertService.getAlertsByRecipient(recipientId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recipient/{recipientId}/unread")
    @Operation(summary = "Get unread alerts for a recipient")
    public ResponseEntity<List<AlertResponseDTO>> getUnreadAlerts(@PathVariable Long recipientId) {
        log.info("Received request to get unread alerts for recipient: {}", recipientId);
        List<AlertResponseDTO> response = alertService.getUnreadAlerts(recipientId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recipient/{recipientId}/critical")
    @Operation(summary = "Get unacknowledged critical alerts")
    public ResponseEntity<List<AlertResponseDTO>> getUnacknowledgedCriticalAlerts(@PathVariable Long recipientId) {
        log.info("Received request to get critical alerts for recipient: {}", recipientId);
        List<AlertResponseDTO> response = alertService.getUnacknowledgedCriticalAlerts(recipientId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recipient/{recipientId}/unread-count")
    @Operation(summary = "Get unread alert count")
    public ResponseEntity<Long> getUnreadCount(@PathVariable Long recipientId) {
        log.info("Received request to get unread count for recipient: {}", recipientId);
        Long count = alertService.getUnreadCount(recipientId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent alerts")
    public ResponseEntity<List<AlertResponseDTO>> getRecentAlerts(
            @RequestParam(defaultValue = "7") int days) {
        log.info("Received request to get alerts from last {} days", days);
        List<AlertResponseDTO> response = alertService.getRecentAlerts(days);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark alert as read")
    public ResponseEntity<String> markAsRead(@PathVariable Long id) {
        log.info("Received request to mark alert as read: {}", id);
        alertService.markAsRead(id);
        return ResponseEntity.ok("Alert marked as read");
    }

    @PutMapping("/recipient/{recipientId}/read-all")
    @Operation(summary = "Mark all alerts as read for a recipient")
    public ResponseEntity<String> markAllAsRead(@PathVariable Long recipientId) {
        log.info("Received request to mark all alerts as read for recipient: {}", recipientId);
        alertService.markAllAsRead(recipientId);
        return ResponseEntity.ok("All alerts marked as read");
    }

    @PutMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge alert")
    public ResponseEntity<String> acknowledgeAlert(@PathVariable Long id) {
        log.info("Received request to acknowledge alert: {}", id);
        alertService.acknowledge(id);
        return ResponseEntity.ok("Alert acknowledged");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete alert")
    public ResponseEntity<String> deleteAlert(@PathVariable Long id) {
        log.info("Received request to delete alert: {}", id);
        alertService.deleteAlert(id);
        return ResponseEntity.ok("Alert deleted successfully");
    }
}