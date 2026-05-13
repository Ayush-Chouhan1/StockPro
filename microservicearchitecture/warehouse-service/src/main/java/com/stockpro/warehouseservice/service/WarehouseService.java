package com.stockpro.warehouseservice.service;

import com.stockpro.warehouseservice.dto.*;
import com.stockpro.warehouseservice.entity.Warehouse;
import com.stockpro.warehouseservice.exception.WarehouseNotFoundException;
import com.stockpro.warehouseservice.repository.WarehouseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class WarehouseService {

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Transactional
    public WarehouseResponseDTO createWarehouse(WarehouseRequestDTO dto) {
        log.info("Creating warehouse: {}", dto.getName());

        if (warehouseRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException(
                    "Warehouse with name '" + dto.getName() + "' already exists");
        }

        Warehouse warehouse = Warehouse.builder()
                .name(dto.getName())
                .location(dto.getLocation())
                .address(dto.getAddress())
                .managerId(dto.getManagerId())
                .capacity(dto.getCapacity())
                .usedCapacity(0)
                .phone(dto.getPhone())
                .isActive(true)
                .build();

        Warehouse saved = warehouseRepository.save(warehouse);
        log.info("Warehouse created with ID: {}", saved.getWarehouseId());
        return mapToDTO(saved);
    }

    public WarehouseResponseDTO getWarehouseById(Long id) {
        log.info("Fetching warehouse with ID: {}", id);
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new WarehouseNotFoundException(
                        "Warehouse not found with ID: " + id));
        return mapToDTO(warehouse);
    }

    public List<WarehouseResponseDTO> getAllWarehouses() {
        log.info("Fetching all warehouses");
        return warehouseRepository.findAll()
                .stream().map(this::mapToDTO).toList();
    }

    public List<WarehouseResponseDTO> getActiveWarehouses() {
        log.info("Fetching active warehouses");
        return warehouseRepository.findByIsActive(true)
                .stream().map(this::mapToDTO).toList();
    }

    public List<WarehouseResponseDTO> getWarehousesByManager(Long managerId) {
        log.info("Fetching warehouses for manager: {}", managerId);
        return warehouseRepository.findByManagerId(managerId)
                .stream().map(this::mapToDTO).toList();
    }

    @Transactional
    public WarehouseResponseDTO updateWarehouse(Long id, WarehouseRequestDTO dto) {
        log.info("Updating warehouse with ID: {}", id);

        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new WarehouseNotFoundException(
                        "Warehouse not found with ID: " + id));

        // Check name uniqueness if changed
        if (!warehouse.getName().equals(dto.getName())
                && warehouseRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException(
                    "Warehouse with name '" + dto.getName() + "' already exists");
        }

        warehouse.setName(dto.getName());
        warehouse.setLocation(dto.getLocation());
        warehouse.setAddress(dto.getAddress());
        warehouse.setManagerId(dto.getManagerId());
        warehouse.setCapacity(dto.getCapacity());
        warehouse.setPhone(dto.getPhone());

        Warehouse updated = warehouseRepository.save(warehouse);
        log.info("Warehouse updated with ID: {}", id);
        return mapToDTO(updated);
    }

    @Transactional
    public void deactivateWarehouse(Long id) {
        log.info("Deactivating warehouse with ID: {}", id);
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new WarehouseNotFoundException(
                        "Warehouse not found with ID: " + id));
        warehouse.setIsActive(false);
        warehouseRepository.save(warehouse);
        log.info("Warehouse deactivated: {}", id);
    }

    @Transactional
    public void assignManager(Long warehouseId, Long managerId) {
        log.info("Assigning manager {} to warehouse {}", managerId, warehouseId);
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new WarehouseNotFoundException(
                        "Warehouse not found with ID: " + warehouseId));
        warehouse.setManagerId(managerId);
        warehouseRepository.save(warehouse);
    }

    private WarehouseResponseDTO mapToDTO(Warehouse w) {
        return WarehouseResponseDTO.builder()
                .warehouseId(w.getWarehouseId())
                .name(w.getName())
                .location(w.getLocation())
                .address(w.getAddress())
                .managerId(w.getManagerId())
                .capacity(w.getCapacity())
                .usedCapacity(w.getUsedCapacity())
                .phone(w.getPhone())
                .isActive(w.getIsActive())
                .createdAt(w.getCreatedAt())
                .build();
    }
}