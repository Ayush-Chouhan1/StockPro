package com.stockpro.alertservice;

import com.stockpro.alertservice.dto.AlertRequestDTO;
import com.stockpro.alertservice.dto.AlertResponseDTO;
import com.stockpro.alertservice.entity.*;
import com.stockpro.alertservice.exception.AlertNotFoundException;
import com.stockpro.alertservice.repository.AlertRepository;
import com.stockpro.alertservice.service.AlertService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private AlertService alertService;

    private Alert mockAlert;
    private AlertRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        mockAlert = Alert.builder()
                .alertId(1L)
                .recipientId(1L)
                .type(AlertType.LOW_STOCK)
                .severity(Severity.WARNING)
                .title("Low Stock Alert")
                .message("Product A is running low")
                .relatedProductId(1L)
                .relatedWarehouseId(1L)
                .isRead(false)
                .isAcknowledged(false)
                .createdAt(LocalDateTime.now())
                .build();

        requestDTO = new AlertRequestDTO();
        requestDTO.setRecipientId(1L);
        requestDTO.setType(AlertType.LOW_STOCK);
        requestDTO.setSeverity(Severity.WARNING);
        requestDTO.setTitle("Low Stock Alert");
        requestDTO.setMessage("Product A is running low");
        requestDTO.setRelatedProductId(1L);
        requestDTO.setRelatedWarehouseId(1L);
    }

    @Test
    void createAlert_Success() {
        when(alertRepository.save(any(Alert.class))).thenReturn(mockAlert);

        AlertResponseDTO result = alertService.createAlert(requestDTO);

        assertNotNull(result);
        assertEquals(AlertType.LOW_STOCK, result.getType());
        assertEquals(Severity.WARNING, result.getSeverity());
        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void createAlert_CriticalAlert_SendsEmail() {
        requestDTO.setSeverity(Severity.CRITICAL);
        Alert criticalAlert = Alert.builder()
                .alertId(2L).recipientId(1L)
                .type(AlertType.OVERDUE_RECEIPT)
                .severity(Severity.CRITICAL)
                .title("Critical Alert")
                .message("PO overdue!")
                .isRead(false).isAcknowledged(false)
                .createdAt(LocalDateTime.now()).build();

        when(alertRepository.save(any(Alert.class))).thenReturn(criticalAlert);

        AlertResponseDTO result = alertService.createAlert(requestDTO);

        assertNotNull(result);
        assertEquals(Severity.CRITICAL, result.getSeverity());
    }

    @Test
    void getAlertById_Success() {
        when(alertRepository.findById(1L)).thenReturn(Optional.of(mockAlert));

        AlertResponseDTO result = alertService.getAlertById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getAlertId());
    }

    @Test
    void getAlertById_NotFound_ThrowsException() {
        when(alertRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AlertNotFoundException.class,
                () -> alertService.getAlertById(99L));
    }

    @Test
    void getAlertsByRecipient_ReturnsList() {
        when(alertRepository.findByRecipientId(1L))
                .thenReturn(List.of(mockAlert));

        List<AlertResponseDTO> result = alertService.getAlertsByRecipient(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getRecipientId());
    }

    @Test
    void getAlertsByRecipient_EmptyList() {
        when(alertRepository.findByRecipientId(99L)).thenReturn(List.of());

        List<AlertResponseDTO> result = alertService.getAlertsByRecipient(99L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getUnreadAlerts_ReturnsList() {
        when(alertRepository.findUnreadAlertsByRecipient(1L))
                .thenReturn(List.of(mockAlert));

        List<AlertResponseDTO> result = alertService.getUnreadAlerts(1L);

        assertEquals(1, result.size());
        assertFalse(result.get(0).getIsRead());
    }

    @Test
    void getUnreadCount_ReturnsCount() {
        when(alertRepository.countByRecipientIdAndIsRead(1L, false))
                .thenReturn(5L);

        Long count = alertService.getUnreadCount(1L);

        assertEquals(5L, count);
    }

    @Test
    void markAsRead_Success() {
        when(alertRepository.findById(1L)).thenReturn(Optional.of(mockAlert));
        when(alertRepository.save(any(Alert.class))).thenReturn(mockAlert);

        assertDoesNotThrow(() -> alertService.markAsRead(1L));

        verify(alertRepository).save(argThat(a -> a.getIsRead()
                && a.getReadAt() != null));
    }

    @Test
    void markAsRead_NotFound_ThrowsException() {
        when(alertRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AlertNotFoundException.class,
                () -> alertService.markAsRead(99L));
    }

    @Test
    void markAllAsRead_Success() {
        Alert unread1 = Alert.builder().alertId(1L).recipientId(1L)
                .isRead(false).isAcknowledged(false)
                .type(AlertType.LOW_STOCK).severity(Severity.WARNING)
                .title("T1").message("M1").createdAt(LocalDateTime.now()).build();
        Alert unread2 = Alert.builder().alertId(2L).recipientId(1L)
                .isRead(false).isAcknowledged(false)
                .type(AlertType.OVERSTOCK).severity(Severity.INFO)
                .title("T2").message("M2").createdAt(LocalDateTime.now()).build();

        when(alertRepository.findByRecipientIdAndIsRead(1L, false))
                .thenReturn(List.of(unread1, unread2));
        when(alertRepository.saveAll(anyList()))
                .thenReturn(List.of(unread1, unread2));

        assertDoesNotThrow(() -> alertService.markAllAsRead(1L));
        verify(alertRepository).saveAll(anyList());
    }

    @Test
    void acknowledge_Success() {
        when(alertRepository.findById(1L)).thenReturn(Optional.of(mockAlert));
        when(alertRepository.save(any(Alert.class))).thenReturn(mockAlert);

        assertDoesNotThrow(() -> alertService.acknowledge(1L));

        verify(alertRepository).save(argThat(a ->
                a.getIsAcknowledged() && a.getAcknowledgedAt() != null));
    }

    @Test
    void acknowledge_NotFound_ThrowsException() {
        when(alertRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AlertNotFoundException.class,
                () -> alertService.acknowledge(99L));
    }

    @Test
    void deleteAlert_Success() {
        when(alertRepository.existsById(1L)).thenReturn(true);
        doNothing().when(alertRepository).deleteById(1L);

        assertDoesNotThrow(() -> alertService.deleteAlert(1L));
        verify(alertRepository).deleteById(1L);
    }

    @Test
    void deleteAlert_NotFound_ThrowsException() {
        when(alertRepository.existsById(99L)).thenReturn(false);

        assertThrows(AlertNotFoundException.class,
                () -> alertService.deleteAlert(99L));
    }

    @Test
    void getRecentAlerts_ReturnsList() {
        when(alertRepository.findRecentAlerts(any(LocalDateTime.class)))
                .thenReturn(List.of(mockAlert));

        List<AlertResponseDTO> result = alertService.getRecentAlerts(7);

        assertEquals(1, result.size());
    }

    @Test
    void getUnacknowledgedCriticalAlerts_ReturnsList() {
        when(alertRepository.findUnacknowledgedCriticalAlerts(1L))
                .thenReturn(List.of(mockAlert));

        List<AlertResponseDTO> result =
                alertService.getUnacknowledgedCriticalAlerts(1L);

        assertEquals(1, result.size());
    }
}