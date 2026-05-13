package com.stockpro.purchaseservice.repository;

import com.stockpro.purchaseservice.entity.POLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface POLineItemRepository
        extends JpaRepository<POLineItem, Long> {

    List<POLineItem> findByPurchaseOrderPoId(Long poId);
}