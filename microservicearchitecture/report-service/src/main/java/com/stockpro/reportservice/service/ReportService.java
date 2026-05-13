package com.stockpro.reportservice.service;

import com.stockpro.reportservice.dto.*;
import com.stockpro.reportservice.entity.InventorySnapshot;
import com.stockpro.reportservice.repository.InventorySnapshotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ReportService {

    @Autowired
    private InventorySnapshotRepository snapshotRepository;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Value("${report.dead-stock-days:90}")
    private int deadStockDays;

    @Value("${report.slow-moving-days:30}")
    private int slowMovingDays;

    @Value("${services.warehouse-url}")
    private String warehouseUrl;

    @Value("${services.movement-url}")
    private String movementUrl;

    @Value("${services.purchase-url}")
    private String purchaseUrl;

    // ==================== SNAPSHOT ====================

    @Transactional
    public InventorySnapshotDTO takeSnapshot(
            Long warehouseId, Long productId,
            Integer quantity, BigDecimal stockValue) {
        log.info("Taking snapshot for product {} in warehouse {}",
                productId, warehouseId);

        LocalDate today = LocalDate.now();

        InventorySnapshot snapshot = snapshotRepository
                .findByWarehouseIdAndProductIdAndSnapshotDate(
                        warehouseId, productId, today)
                .orElseGet(() -> InventorySnapshot.builder()
                        .warehouseId(warehouseId)
                        .productId(productId)
                        .snapshotDate(today)
                        .build());

        snapshot.setQuantity(quantity);
        snapshot.setStockValue(stockValue);

        InventorySnapshot saved = snapshotRepository.save(snapshot);
        log.info("Snapshot saved with ID: {}", saved.getSnapshotId());
        return mapToDTO(saved);
    }

    public List<InventorySnapshotDTO> getSnapshotsByDate(LocalDate date) {
        log.info("Fetching snapshots for date: {}", date);
        return snapshotRepository.findBySnapshotDate(date)
                .stream().map(this::mapToDTO).toList();
    }

    public List<InventorySnapshotDTO> getSnapshotsByWarehouse(Long warehouseId) {
        return snapshotRepository.findByWarehouseId(warehouseId)
                .stream().map(this::mapToDTO).toList();
    }

    public List<InventorySnapshotDTO> getSnapshotsByDateRange(
            LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException(
                    "Start date cannot be after end date");
        }
        return snapshotRepository.findBySnapshotDateBetween(start, end)
                .stream().map(this::mapToDTO).toList();
    }

    public List<InventorySnapshotDTO> getLatestSnapshot() {
        return snapshotRepository.findLatestSnapshot()
                .stream().map(this::mapToDTO).toList();
    }

    // ==================== VALUATION ====================

    public StockValuationDTO getTotalStockValue() {
        log.info("Calculating total stock value");
        LocalDate today = LocalDate.now();
        BigDecimal total = snapshotRepository.sumTotalStockValue(today);
        List<InventorySnapshot> latest = snapshotRepository.findLatestSnapshot();

        return StockValuationDTO.builder()
                .totalValue(total != null ? total : BigDecimal.ZERO)
                .asOfDate(today)
                .totalProducts(latest.size())
                .build();
    }

    public StockValuationDTO getStockValueByWarehouse(Long warehouseId) {
        log.info("Calculating stock value for warehouse: {}", warehouseId);
        LocalDate today = LocalDate.now();
        BigDecimal value = snapshotRepository
                .sumStockValueByWarehouse(warehouseId, today);
        List<InventorySnapshot> warehouseSnapshots =
                snapshotRepository.findByWarehouseIdAndSnapshotDate(
                        warehouseId, today);

        return StockValuationDTO.builder()
                .warehouseId(warehouseId)
                .totalValue(value != null ? value : BigDecimal.ZERO)
                .asOfDate(today)
                .totalProducts(warehouseSnapshots.size())
                .build();
    }

    // ==================== INVENTORY TURNOVER ====================

    public Map<String, Object> getInventoryTurnover(
            LocalDate startDate, LocalDate endDate) {
        log.info("Calculating inventory turnover from {} to {}",
                startDate, endDate);

        BigDecimal startValue = snapshotRepository.sumTotalStockValue(startDate);
        BigDecimal endValue = snapshotRepository.sumTotalStockValue(endDate);

        if (startValue == null) startValue = BigDecimal.ZERO;
        if (endValue == null) endValue = BigDecimal.ZERO;

        BigDecimal avgInventory = startValue.add(endValue)
                .divide(BigDecimal.valueOf(2));

        return Map.of(
                "startDate", startDate,
                "endDate", endDate,
                "startInventoryValue", startValue,
                "endInventoryValue", endValue,
                "averageInventoryValue", avgInventory,
                "note", "Turnover ratio = COGS / Average Inventory Value"
        );
    }

    // ==================== LOW STOCK REPORT ====================

    public List<InventorySnapshotDTO> getLowStockReport(Integer threshold) {
        log.info("Generating low stock report with threshold: {}", threshold);
        return snapshotRepository.findLatestSnapshot().stream()
                .filter(s -> s.getQuantity() <= threshold)
                .map(this::mapToDTO)
                .toList();
    }

    // ==================== DEAD STOCK ====================

    public List<DeadStockDTO> getDeadStock(Integer days) {
        log.info("Finding dead stock (no movement for {} days)", days);
        int threshold = (days != null) ? days : deadStockDays;
        LocalDate cutoffDate = LocalDate.now().minusDays(threshold);

        return snapshotRepository.findLatestSnapshot().stream()
                // FIX: && not || — product must BOTH be old AND have stock sitting
                .filter(s -> s.getSnapshotDate().isBefore(cutoffDate)
                        && s.getQuantity() > 0)
                .map(s -> DeadStockDTO.builder()
                        .productId(s.getProductId())
                        .warehouseId(s.getWarehouseId())
                        .currentQuantity(s.getQuantity())
                        .lastMovementDate(s.getSnapshotDate())
                        .daysWithoutMovement(
                                (int) (LocalDate.now().toEpochDay()
                                - s.getSnapshotDate().toEpochDay()))
                        .build())
                .filter(d -> d.getDaysWithoutMovement() >= threshold)
                .toList();
    }

    // ==================== PO SUMMARY ====================

    public POSummaryDTO getPOSummary(LocalDate startDate, LocalDate endDate) {
        log.info("Generating PO summary from {} to {}", startDate, endDate);

        try {
            List<?> pos = webClientBuilder.build()
                    .get()
                    .uri(purchaseUrl + "/purchase-orders/date-range"
                            + "?startDate=" + startDate
                            + "&endDate=" + endDate)
                    .retrieve()
                    .bodyToFlux(Map.class)
                    .collectList()
                    .block();

            if (pos == null) pos = List.of();

            long total = pos.size();
            long approved = pos.stream()
                    .filter(p -> "APPROVED".equals(
                            ((Map<?, ?>) p).get("status")))
                    .count();
            long pending = pos.stream()
                    .filter(p -> "PENDING".equals(
                            ((Map<?, ?>) p).get("status")))
                    .count();
            long cancelled = pos.stream()
                    .filter(p -> "CANCELLED".equals(
                            ((Map<?, ?>) p).get("status")))
                    .count();
            long received = pos.stream()
                    .filter(p -> "FULLY_RECEIVED".equals(
                            ((Map<?, ?>) p).get("status")))
                    .count();

            BigDecimal totalSpend = pos.stream()
                    .map(p -> {
                        Object amt = ((Map<?, ?>) p).get("totalAmount");
                        if (amt == null) return BigDecimal.ZERO;
                        return new BigDecimal(amt.toString());
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return POSummaryDTO.builder()
                    .fromDate(startDate)
                    .toDate(endDate)
                    .totalPOs(total)
                    .totalSpend(totalSpend)
                    .approvedPOs(approved)
                    .pendingPOs(pending)
                    .cancelledPOs(cancelled)
                    .fullyReceivedPOs(received)
                    .build();

        } catch (Exception e) {
            log.error("Failed to fetch PO data: {}", e.getMessage());
            return POSummaryDTO.builder()
                    .fromDate(startDate)
                    .toDate(endDate)
                    .totalPOs(0L)
                    .totalSpend(BigDecimal.ZERO)
                    .build();
        }
    }

    // ==================== MOVEMENT SUMMARY ====================

    public Map<String, Object> getStockMovementSummary(
            Long warehouseId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating movement summary");
        return Map.of(
                "warehouseId", warehouseId != null ? warehouseId : "ALL",
                "fromDate", startDate,
                "toDate", endDate,
                "note", "Detailed movement data available via movement-service /movements/date-range"
        );
    }

    // ==================== TOP / SLOW MOVERS ====================

    public List<TopMovingProductDTO> getTopMovingProducts(Integer limit) {
        log.info("Fetching top {} moving products", limit);
        return snapshotRepository.findLatestSnapshot().stream()
                .sorted((a, b) -> b.getStockValue()
                        .compareTo(a.getStockValue()))
                .limit(limit != null ? limit : 10)
                .map(s -> TopMovingProductDTO.builder()
                        .productId(s.getProductId())
                        .warehouseId(s.getWarehouseId())
                        .totalUnitsIn(s.getQuantity())
                        .totalUnitsOut(0)
                        .totalMovement(s.getQuantity())
                        .build())
                .toList();
    }

    public List<TopMovingProductDTO> getSlowMovingProducts(Integer days) {
        log.info("Fetching slow moving products");
        int threshold = (days != null) ? days : slowMovingDays;
        LocalDate cutoff = LocalDate.now().minusDays(threshold);

        return snapshotRepository.findLatestSnapshot().stream()
                .filter(s -> s.getSnapshotDate().isBefore(cutoff))
                .map(s -> TopMovingProductDTO.builder()
                        .productId(s.getProductId())
                        .warehouseId(s.getWarehouseId())
                        .totalUnitsIn(s.getQuantity())
                        .totalUnitsOut(0)
                        .totalMovement(0)
                        .build())
                .toList();
    }

    // ==================== HELPER ====================

    private InventorySnapshotDTO mapToDTO(InventorySnapshot s) {
        return InventorySnapshotDTO.builder()
                .snapshotId(s.getSnapshotId())
                .warehouseId(s.getWarehouseId())
                .productId(s.getProductId())
                .quantity(s.getQuantity())
                .stockValue(s.getStockValue())
                .snapshotDate(s.getSnapshotDate())
                .createdAt(s.getCreatedAt())
                .build();
    }
}