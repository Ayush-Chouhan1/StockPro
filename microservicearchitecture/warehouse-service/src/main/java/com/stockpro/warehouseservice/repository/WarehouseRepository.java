package com.stockpro.warehouseservice.repository;

import com.stockpro.warehouseservice.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    List<Warehouse> findByIsActive(Boolean isActive);

    List<Warehouse> findByManagerId(Long managerId);

    Optional<Warehouse> findByName(String name);

    boolean existsByName(String name);

    long countByIsActive(Boolean isActive);
}