package com.stockpro.warehouseservice.repository;

import com.stockpro.warehouseservice.entity.StockLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockLevelRepository extends JpaRepository<StockLevel, Long> {

    Optional<StockLevel> findByWarehouseIdAndProductId(
            Long warehouseId, Long productId);

    List<StockLevel> findByWarehouseId(Long warehouseId);

    List<StockLevel> findByProductId(Long productId);

    // Products below reorder level (used by alert-service)
    @Query("SELECT s FROM StockLevel s WHERE s.quantity <= :reorderLevel")
    List<StockLevel> findLowStockItems(@Param("reorderLevel") Integer reorderLevel);

    // All stock entries where available quantity is 0 or less
    @Query("SELECT s FROM StockLevel s WHERE (s.quantity - s.reservedQuantity) <= 0")
    List<StockLevel> findOutOfStockItems();

    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM StockLevel s WHERE s.warehouseId = :warehouseId")
    Long sumQuantityByWarehouseId(@Param("warehouseId") Long warehouseId);

    boolean existsByWarehouseIdAndProductId(Long warehouseId, Long productId);
}
