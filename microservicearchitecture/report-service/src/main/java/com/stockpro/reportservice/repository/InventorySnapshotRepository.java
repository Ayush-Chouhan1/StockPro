package com.stockpro.reportservice.repository;

import com.stockpro.reportservice.entity.InventorySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventorySnapshotRepository
        extends JpaRepository<InventorySnapshot, Long> {

    List<InventorySnapshot> findByWarehouseId(Long warehouseId);

    List<InventorySnapshot> findByProductId(Long productId);

    List<InventorySnapshot> findBySnapshotDate(LocalDate snapshotDate);

    List<InventorySnapshot> findBySnapshotDateBetween(
            LocalDate start, LocalDate end);

    List<InventorySnapshot> findByWarehouseIdAndSnapshotDate(
            Long warehouseId, LocalDate snapshotDate);

    Optional<InventorySnapshot> findByWarehouseIdAndProductIdAndSnapshotDate(
            Long warehouseId, Long productId, LocalDate snapshotDate);

    // Total stock value across all warehouses for a date
    @Query("SELECT COALESCE(SUM(s.stockValue), 0) " +
           "FROM InventorySnapshot s WHERE s.snapshotDate = :date")
    BigDecimal sumTotalStockValue(@Param("date") LocalDate date);

    // Total stock value per warehouse
    @Query("SELECT COALESCE(SUM(s.stockValue), 0) " +
           "FROM InventorySnapshot s " +
           "WHERE s.warehouseId = :warehouseId " +
           "AND s.snapshotDate = :date")
    BigDecimal sumStockValueByWarehouse(
            @Param("warehouseId") Long warehouseId,
            @Param("date") LocalDate date);

    // Latest snapshot per product per warehouse
    @Query("SELECT s FROM InventorySnapshot s " +
           "WHERE s.snapshotDate = " +
           "(SELECT MAX(s2.snapshotDate) FROM InventorySnapshot s2)")
    List<InventorySnapshot> findLatestSnapshot();

    boolean existsByWarehouseIdAndProductIdAndSnapshotDate(
            Long warehouseId, Long productId, LocalDate snapshotDate);
}