package com.stockpro.alertservice.service;

import com.stockpro.alertservice.dto.AlertRequestDTO;
import com.stockpro.alertservice.dto.AlertResponseDTO;
import com.stockpro.alertservice.entity.Alert;
import com.stockpro.alertservice.entity.Severity;
import com.stockpro.alertservice.exception.AlertNotFoundException;
import com.stockpro.alertservice.repository.AlertRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AlertService {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@stockpro.com}")
    private String fromEmail;

    @Transactional
    public AlertResponseDTO createAlert(AlertRequestDTO dto) {
        log.info("Creating alert for recipient: {}", dto.getRecipientId());

        Alert alert = mapToEntity(dto);
        Alert savedAlert = alertRepository.save(alert);

        if (shouldSendEmail(alert) && mailSender != null) {
            sendEmailNotification(alert);
        }

        log.info("Alert created successfully with ID: {}", savedAlert.getAlertId());
        return mapToDTO(savedAlert);
    }

    public AlertResponseDTO getAlertById(Long id) {
        log.info("Fetching alert with ID: {}", id);
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new AlertNotFoundException("Alert not found with ID: " + id));
        return mapToDTO(alert);
    }

    public List<AlertResponseDTO> getAlertsByRecipient(Long recipientId) {
        log.info("Fetching alerts for recipient: {}", recipientId);
        return alertRepository.findByRecipientId(recipientId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<AlertResponseDTO> getUnreadAlerts(Long recipientId) {
        log.info("Fetching unread alerts for recipient: {}", recipientId);
        return alertRepository.findUnreadAlertsByRecipient(recipientId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<AlertResponseDTO> getUnacknowledgedCriticalAlerts(Long recipientId) {
        log.info("Fetching unacknowledged critical alerts for recipient: {}", recipientId);
        return alertRepository.findUnacknowledgedCriticalAlerts(recipientId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public Long getUnreadCount(Long recipientId) {
        log.info("Getting unread count for recipient: {}", recipientId);
        return alertRepository.countByRecipientIdAndIsRead(recipientId, false);
    }

    @Transactional
    public void markAsRead(Long alertId) {
        log.info("Marking alert as read: {}", alertId);

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new AlertNotFoundException("Alert not found with ID: " + alertId));

        alert.setIsRead(true);
        alert.setReadAt(LocalDateTime.now());
        alertRepository.save(alert);

        log.info("Alert marked as read: {}", alertId);
    }

    @Transactional
    public void markAllAsRead(Long recipientId) {
        log.info("Marking all alerts as read for recipient: {}", recipientId);

        List<Alert> unreadAlerts = alertRepository.findByRecipientIdAndIsRead(recipientId, false);
        unreadAlerts.forEach(alert -> {
            alert.setIsRead(true);
            alert.setReadAt(LocalDateTime.now());
        });
        alertRepository.saveAll(unreadAlerts);

        log.info("Marked {} alerts as read", unreadAlerts.size());
    }

    @Transactional
    public void acknowledge(Long alertId) {
        log.info("Acknowledging alert: {}", alertId);

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new AlertNotFoundException("Alert not found with ID: " + alertId));

        alert.setIsAcknowledged(true);
        alert.setAcknowledgedAt(LocalDateTime.now());
        alertRepository.save(alert);

        log.info("Alert acknowledged: {}", alertId);
    }

    @Transactional
    public void deleteAlert(Long alertId) {
        log.info("Deleting alert: {}", alertId);

        if (!alertRepository.existsById(alertId)) {
            throw new AlertNotFoundException("Alert not found with ID: " + alertId);
        }

        alertRepository.deleteById(alertId);
        log.info("Alert deleted: {}", alertId);
    }

    public List<AlertResponseDTO> getRecentAlerts(int days) {
        log.info("Fetching alerts from last {} days", days);
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        return alertRepository.findRecentAlerts(startDate)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // Helper method to send email
    private void sendEmailNotification(Alert alert) {
        try {
            if (mailSender == null) {
                log.warn("Mail sender not configured, skipping email for alert: {}", alert.getAlertId());
                return;
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(alert.getRecipientEmail());
            message.setSubject("[" + alert.getSeverity() + "] " + alert.getTitle());
            message.setText(alert.getMessage());

            mailSender.send(message);
            log.info("Email sent for critical alert: {}", alert.getAlertId());
        } catch (Exception e) {
            log.error("Failed to send email for alert: {}", alert.getAlertId(), e);
            // Don't fail the alert creation if email fails
        }
    }

    // Mapping methods
    private Alert mapToEntity(AlertRequestDTO dto) {
        return Alert.builder()
                .recipientId(dto.getRecipientId())
                .recipientEmail(dto.getRecipientEmail())
                .type(dto.getType())
                .severity(dto.getSeverity())
                .title(dto.getTitle())
                .message(dto.getMessage())
                .relatedProductId(dto.getRelatedProductId())
                .relatedWarehouseId(dto.getRelatedWarehouseId())
                .relatedPurchaseOrderId(dto.getRelatedPurchaseOrderId())
                .channel(dto.getChannel())
                .isRead(false)
                .isAcknowledged(false)
                .build();
    }

    private boolean shouldSendEmail(Alert alert) {
        String channel = alert.getChannel();
        return alert.getRecipientEmail() != null
                && !alert.getRecipientEmail().isBlank()
                && (alert.getSeverity() == Severity.CRITICAL
                        || (channel != null && channel.contains("EMAIL")));
    }

    private AlertResponseDTO mapToDTO(Alert alert) {
        return AlertResponseDTO.builder()
                .alertId(alert.getAlertId())
                .recipientId(alert.getRecipientId())
                .type(alert.getType())
                .severity(alert.getSeverity())
                .title(alert.getTitle())
                .message(alert.getMessage())
                .relatedProductId(alert.getRelatedProductId())
                .relatedWarehouseId(alert.getRelatedWarehouseId())
                .relatedPurchaseOrderId(alert.getRelatedPurchaseOrderId())
                .channel(alert.getChannel())
                .recipientEmail(alert.getRecipientEmail())
                .isRead(alert.getIsRead())
                .isAcknowledged(alert.getIsAcknowledged())
                .readAt(alert.getReadAt())
                .acknowledgedAt(alert.getAcknowledgedAt())
                .createdAt(alert.getCreatedAt())
                .build();
    }
}
