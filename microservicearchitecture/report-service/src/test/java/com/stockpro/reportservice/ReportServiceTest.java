package com.stockpro.reportservice;

import com.stockpro.reportservice.dto.*;
import com.stockpro.reportservice.entity.InventorySnapshot;
import com.stockpro.reportservice.repository.InventorySnapshotRepository;
import com.stockpro.reportservice.service.ReportService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private InventorySnapshotRepository snapshotRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    @InjectMocks
    private ReportService reportService;

    private InventorySnapshot mockSnapshot;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reportService, "deadStockDays", 90);
        ReflectionTestUtils.setField(reportService, "slowMovingDays", 30);
        ReflectionTestUtils.setField(reportService,
                "warehouseUrl", "http://localhost:8084");
        ReflectionTestUtils.setField(reportService,
                "movementUrl", "http://localhost:8086");
        ReflectionTestUtils.setField(reportService,
                "purchaseUrl", "http://localhost:8085");

        mockSnapshot = InventorySnapshot.builder()
                .snapshotId(1L)
                .warehouseId(1L)
                .productId(1L)
                .quantity(100)
                .stockValue(new BigDecimal("5000.00"))
                .snapshotDate(LocalDate.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void takeSnapshot_NewSnapshot_Success() {
        when(snapshotRepository
                .findByWarehouseIdAndProductIdAndSnapshotDate(
                        1L, 1L, LocalDate.now()))
                .thenReturn(Optional.empty());
        when(snapshotRepository.save(any(InventorySnapshot.class)))
                .thenReturn(mockSnapshot);

        InventorySnapshotDTO result = reportService.takeSnapshot(
                1L, 1L, 100, new BigDecimal("5000.00"));

        assertNotNull(result);
        assertEquals(1L, result.getWarehouseId());
        assertEquals(100, result.getQuantity());
        verify(snapshotRepository).save(any(InventorySnapshot.class));
    }

    @Test
    void takeSnapshot_ExistingSnapshot_Updates() {
        when(snapshotRepository
                .findByWarehouseIdAndProductIdAndSnapshotDate(
                        1L, 1L, LocalDate.now()))
                .thenReturn(Optional.of(mockSnapshot));
        when(snapshotRepository.save(any(InventorySnapshot.class)))
                .thenReturn(mockSnapshot);

        InventorySnapshotDTO result = reportService.takeSnapshot(
                1L, 1L, 150, new BigDecimal("7500.00"));

        assertNotNull(result);
        verify(snapshotRepository).save(any(InventorySnapshot.class));
    }

    @Test
    void getSnapshotsByDate_ReturnsList() {
        when(snapshotRepository.findBySnapshotDate(LocalDate.now()))
                .thenReturn(List.of(mockSnapshot));

        List<InventorySnapshotDTO> result =
                reportService.getSnapshotsByDate(LocalDate.now());

        assertEquals(1, result.size());
    }

    @Test
    void getSnapshotsByWarehouse_ReturnsList() {
        when(snapshotRepository.findByWarehouseId(1L))
                .thenReturn(List.of(mockSnapshot));

        List<InventorySnapshotDTO> result =
                reportService.getSnapshotsByWarehouse(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getWarehouseId());
    }

    @Test
    void getSnapshotsByDateRange_ValidRange_ReturnsList() {
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();
        when(snapshotRepository.findBySnapshotDateBetween(start, end))
                .thenReturn(List.of(mockSnapshot));

        List<InventorySnapshotDTO> result =
                reportService.getSnapshotsByDateRange(start, end);

        assertEquals(1, result.size());
    }

    @Test
    void getSnapshotsByDateRange_InvalidRange_ThrowsException() {
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().minusDays(7);

        assertThrows(IllegalArgumentException.class,
                () -> reportService.getSnapshotsByDateRange(start, end));
    }

    @Test
    void getLatestSnapshot_ReturnsList() {
        when(snapshotRepository.findLatestSnapshot())
                .thenReturn(List.of(mockSnapshot));

        List<InventorySnapshotDTO> result = reportService.getLatestSnapshot();

        assertEquals(1, result.size());
    }

    @Test
    void getTotalStockValue_Success() {
        when(snapshotRepository.sumTotalStockValue(LocalDate.now()))
                .thenReturn(new BigDecimal("50000.00"));
        when(snapshotRepository.findLatestSnapshot())
                .thenReturn(List.of(mockSnapshot));

        StockValuationDTO result = reportService.getTotalStockValue();

        assertNotNull(result);
        assertEquals(new BigDecimal("50000.00"), result.getTotalValue());
        assertEquals(1, result.getTotalProducts());
    }

    @Test
    void getTotalStockValue_NullValue_ReturnsZero() {
        when(snapshotRepository.sumTotalStockValue(LocalDate.now()))
                .thenReturn(null);
        when(snapshotRepository.findLatestSnapshot())
                .thenReturn(List.of());

        StockValuationDTO result = reportService.getTotalStockValue();

        assertEquals(BigDecimal.ZERO, result.getTotalValue());
    }

    @Test
    void getStockValueByWarehouse_Success() {
        when(snapshotRepository.sumStockValueByWarehouse(1L, LocalDate.now()))
                .thenReturn(new BigDecimal("10000.00"));
        when(snapshotRepository.findByWarehouseIdAndSnapshotDate(
                1L, LocalDate.now()))
                .thenReturn(List.of(mockSnapshot));

        StockValuationDTO result =
                reportService.getStockValueByWarehouse(1L);

        assertNotNull(result);
        assertEquals(new BigDecimal("10000.00"), result.getTotalValue());
    }

    @Test
    void getStockValueByWarehouse_NullValue_ReturnsZero() {
        when(snapshotRepository.sumStockValueByWarehouse(1L, LocalDate.now()))
                .thenReturn(null);
        when(snapshotRepository.findByWarehouseIdAndSnapshotDate(
                1L, LocalDate.now()))
                .thenReturn(List.of());

        StockValuationDTO result =
                reportService.getStockValueByWarehouse(1L);

        assertEquals(BigDecimal.ZERO, result.getTotalValue());
        assertEquals(0, result.getTotalProducts());
    }

    @Test
    void getInventoryTurnover_Success() {
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();

        when(snapshotRepository.sumTotalStockValue(start))
                .thenReturn(new BigDecimal("40000.00"));
        when(snapshotRepository.sumTotalStockValue(end))
                .thenReturn(new BigDecimal("50000.00"));

        Map<String, Object> result =
                reportService.getInventoryTurnover(start, end);

        assertNotNull(result);
        assertTrue(result.containsKey("averageInventoryValue"));
        assertEquals(0, new BigDecimal("45000.00")
                .compareTo((BigDecimal) result.get("averageInventoryValue")));
    }

    @Test
    void getInventoryTurnover_NullValues_UsesZero() {
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();

        when(snapshotRepository.sumTotalStockValue(start)).thenReturn(null);
        when(snapshotRepository.sumTotalStockValue(end)).thenReturn(null);

        Map<String, Object> result =
                reportService.getInventoryTurnover(start, end);

        assertEquals(BigDecimal.ZERO, result.get("startInventoryValue"));
        assertEquals(BigDecimal.ZERO, result.get("endInventoryValue"));
        assertEquals(0, BigDecimal.ZERO.compareTo((BigDecimal) result.get("averageInventoryValue")));
    }

    @Test
    void getLowStockReport_ReturnsFilteredList() {
        InventorySnapshot lowStock = InventorySnapshot.builder()
                .snapshotId(2L).warehouseId(1L).productId(2L)
                .quantity(3).stockValue(new BigDecimal("150.00"))
                .snapshotDate(LocalDate.now()).build();

        when(snapshotRepository.findLatestSnapshot())
                .thenReturn(List.of(mockSnapshot, lowStock));

        List<InventorySnapshotDTO> result =
                reportService.getLowStockReport(5);

        // Only lowStock has quantity <= 5
        assertEquals(1, result.size());
        assertEquals(3, result.get(0).getQuantity());
    }

    @Test
    void getTopMovingProducts_ReturnsLimitedList() {
        when(snapshotRepository.findLatestSnapshot())
                .thenReturn(List.of(mockSnapshot));

        List<TopMovingProductDTO> result =
                reportService.getTopMovingProducts(5);

        assertNotNull(result);
        assertTrue(result.size() <= 5);
    }

    @Test
    void getTopMovingProducts_NullLimit_UsesDefault() {
        when(snapshotRepository.findLatestSnapshot())
                .thenReturn(List.of(mockSnapshot));

        List<TopMovingProductDTO> result =
                reportService.getTopMovingProducts(null);

        assertNotNull(result);
    }

    @Test
    void getDeadStock_ReturnsItems() {
        InventorySnapshot old = InventorySnapshot.builder()
                .snapshotId(2L).warehouseId(1L).productId(2L)
                .quantity(10).stockValue(new BigDecimal("500.00"))
                .snapshotDate(LocalDate.now().minusDays(100))
                .build();

        when(snapshotRepository.findLatestSnapshot())
                .thenReturn(List.of(old));

        List<DeadStockDTO> result = reportService.getDeadStock(90);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).getDaysWithoutMovement() >= 90);
    }

    @Test
    void getDeadStock_NullDaysUsesDefaultAndFiltersZeroQuantity() {
        InventorySnapshot oldWithStock = InventorySnapshot.builder()
                .snapshotId(2L).warehouseId(1L).productId(2L)
                .quantity(10).stockValue(new BigDecimal("500.00"))
                .snapshotDate(LocalDate.now().minusDays(100))
                .build();
        InventorySnapshot oldWithoutStock = InventorySnapshot.builder()
                .snapshotId(3L).warehouseId(1L).productId(3L)
                .quantity(0).stockValue(BigDecimal.ZERO)
                .snapshotDate(LocalDate.now().minusDays(100))
                .build();

        when(snapshotRepository.findLatestSnapshot())
                .thenReturn(List.of(oldWithStock, oldWithoutStock, mockSnapshot));

        List<DeadStockDTO> result = reportService.getDeadStock(null);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getProductId());
    }

    @Test
    void getPOSummary_WebClientSuccess_AggregatesStatusesAndSpend() {
        mockPurchaseWebClient(Flux.just(
                Map.of("status", "APPROVED", "totalAmount", "100.50"),
                Map.of("status", "PENDING", "totalAmount", "50.00"),
                Map.of("status", "CANCELLED", "totalAmount", "25.25"),
                Map.of("status", "FULLY_RECEIVED", "totalAmount", "10.00"),
                Map.of("status", "DRAFT")
        ));
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();

        POSummaryDTO result = reportService.getPOSummary(start, end);

        assertEquals(5L, result.getTotalPOs());
        assertEquals(1L, result.getApprovedPOs());
        assertEquals(1L, result.getPendingPOs());
        assertEquals(1L, result.getCancelledPOs());
        assertEquals(1L, result.getFullyReceivedPOs());
        assertEquals(0, new BigDecimal("185.75").compareTo(result.getTotalSpend()));
    }

    @Test
    void getPOSummary_WebClientReturnsNull_UsesEmptyList() {
        mockPurchaseWebClient(Flux.empty());
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();

        POSummaryDTO result = reportService.getPOSummary(start, end);

        assertEquals(0L, result.getTotalPOs());
        assertEquals(BigDecimal.ZERO, result.getTotalSpend());
    }

    @Test
    void getPOSummary_WebClientFailure_ReturnsEmptySummary() {
        when(webClientBuilder.build()).thenThrow(new RuntimeException("purchase unavailable"));
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();

        POSummaryDTO result = reportService.getPOSummary(start, end);

        assertEquals(0L, result.getTotalPOs());
        assertEquals(BigDecimal.ZERO, result.getTotalSpend());
    }

    @Test
    void getStockMovementSummary_ReturnsWarehouseOrAll() {
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();

        Map<String, Object> warehouseSummary =
                reportService.getStockMovementSummary(1L, start, end);
        Map<String, Object> allSummary =
                reportService.getStockMovementSummary(null, start, end);

        assertEquals(1L, warehouseSummary.get("warehouseId"));
        assertEquals("ALL", allSummary.get("warehouseId"));
        assertEquals(start, warehouseSummary.get("fromDate"));
        assertEquals(end, warehouseSummary.get("toDate"));
    }

    @Test
    void getSlowMovingProducts_ReturnsList() {
        InventorySnapshot old = InventorySnapshot.builder()
                .snapshotId(2L).warehouseId(1L).productId(2L)
                .quantity(5).stockValue(new BigDecimal("250.00"))
                .snapshotDate(LocalDate.now().minusDays(45))
                .build();

        when(snapshotRepository.findLatestSnapshot())
                .thenReturn(List.of(old));

        List<TopMovingProductDTO> result =
                reportService.getSlowMovingProducts(30);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void getSlowMovingProducts_NullDaysUsesDefault() {
        InventorySnapshot old = InventorySnapshot.builder()
                .snapshotId(2L).warehouseId(1L).productId(2L)
                .quantity(5).stockValue(new BigDecimal("250.00"))
                .snapshotDate(LocalDate.now().minusDays(45))
                .build();

        when(snapshotRepository.findLatestSnapshot())
                .thenReturn(List.of(old, mockSnapshot));

        List<TopMovingProductDTO> result =
                reportService.getSlowMovingProducts(null);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getProductId());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void mockPurchaseWebClient(Flux<Map> responseFlux) {
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestHeadersUriSpec request = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.ResponseSpec response = mock(WebClient.ResponseSpec.class);

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(request);
        when(request.uri(anyString())).thenReturn(request);
        when(request.retrieve()).thenReturn(response);
        when(response.bodyToFlux(Map.class)).thenReturn(responseFlux);
    }
}
