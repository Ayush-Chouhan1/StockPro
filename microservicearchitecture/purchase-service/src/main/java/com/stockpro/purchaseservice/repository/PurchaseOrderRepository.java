package com.stockpro.purchaseservice.repository;

import com.stockpro.purchaseservice.entity.POStatus;
import com.stockpro.purchaseservice.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PurchaseOrderRepository
        extends JpaRepository<PurchaseOrder, Long> {

    List<PurchaseOrder> findBySupplierId(Long supplierId);

    List<PurchaseOrder> findByWarehouseId(Long warehouseId);

    List<PurchaseOrder> findByStatus(POStatus status);

    List<PurchaseOrder> findByCreatedById(Long createdById);

    List<PurchaseOrder> findByOrderDateBetween(
            LocalDate startDate, LocalDate endDate);

    List<PurchaseOrder> findBySupplierIdAndStatus(
            Long supplierId, POStatus status);

    long countByStatus(POStatus status);

    // Overdue POs - approved but expected date passed
    List<PurchaseOrder> findByStatusAndExpectedDateBefore(
            POStatus status, LocalDate date);
}