package com.stockpro.supplierservice.service;

import com.stockpro.supplierservice.dto.SupplierRequestDTO;
import com.stockpro.supplierservice.dto.SupplierResponseDTO;
import com.stockpro.supplierservice.entity.Supplier;
import com.stockpro.supplierservice.exception.DuplicateTaxIdException;
import com.stockpro.supplierservice.exception.SupplierNotFoundException;
import com.stockpro.supplierservice.repository.SupplierRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;

    @Transactional
    public SupplierResponseDTO createSupplier(SupplierRequestDTO dto) {
        log.info("Creating supplier with email: {}", dto.getEmail());

        if (supplierRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateTaxIdException
            ("Supplier with email " + dto.getEmail() + " already exists");
        }

        if (dto.getTaxId() != null && supplierRepository.existsByTaxId(dto.getTaxId())) {
            throw new DuplicateTaxIdException
            ("Supplier with Tax ID " + dto.getTaxId() + " already exists");
        }

        Supplier supplier = mapToEntity(dto);
        Supplier savedSupplier = supplierRepository.save(supplier);

        log.info("Supplier created successfully with ID: {}", savedSupplier.getSupplierId());
        return mapToDTO(savedSupplier);
    }

    public SupplierResponseDTO getSupplierById(Long id) {
        log.info("Fetching supplier with ID: {}", id);
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new SupplierNotFoundException("Supplier not found with ID: " + id));
        return mapToDTO(supplier);
    }

    public List<SupplierResponseDTO> getAllSuppliers() {
        log.info("Fetching all suppliers");
        return supplierRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<SupplierResponseDTO> getActiveSuppliers() {
        log.info("Fetching active suppliers");
        return supplierRepository.findByIsActive(true)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<SupplierResponseDTO> getSuppliersByCity(String city) {
        log.info("Fetching suppliers by city: {}", city);
        return supplierRepository.findByCity(city)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<SupplierResponseDTO> getSuppliersByCountry(String country) {
        log.info("Fetching suppliers by country: {}", country);
        return supplierRepository.findByCountry(country)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<SupplierResponseDTO> searchSuppliers(String keyword) {
        log.info("Searching suppliers with keyword: {}", keyword);
        return supplierRepository.searchSuppliers(keyword)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<SupplierResponseDTO> getTopRatedSuppliers(Double minRating) {
        log.info("Fetching top-rated suppliers with minimum rating: {}", minRating);
        return supplierRepository.findTopRatedSuppliers(minRating)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public SupplierResponseDTO updateSupplier(Long id, SupplierRequestDTO dto) {
        log.info("Updating supplier with ID: {}", id);

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new SupplierNotFoundException
                		("Supplier not found with ID: " + id));

        // Check email uniqueness if changed
        if (!supplier.getEmail().equals(dto.getEmail()) && supplierRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateTaxIdException
            ("Supplier with email " + dto.getEmail() + " already exists");
        }

        // Check taxId uniqueness if changed
        if (dto.getTaxId() != null && 
            !dto.getTaxId().equals(supplier.getTaxId()) && 
            supplierRepository.existsByTaxId(dto.getTaxId())) {
            throw new DuplicateTaxIdException
            ("Supplier with Tax ID " + dto.getTaxId() + " already exists");
        }

        updateEntityFromDTO(supplier, dto);
        Supplier updatedSupplier = supplierRepository.save(supplier);

        log.info("Supplier updated successfully with ID: {}", id);
        return mapToDTO(updatedSupplier);
    }

    @Transactional
    public void updateRating(Long id, Double newRating) {
        log.info("Updating rating for supplier ID: {} to {}", id, newRating);

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new SupplierNotFoundException
                		("Supplier not found with ID: " + id));

        // Calculate average rating based on total orders
        if (supplier.getTotalOrders() == 0) {
            supplier.setRating(newRating);
        } else {
            double currentTotal = supplier.getRating() * supplier.getTotalOrders();
            double newTotal = currentTotal + newRating;
            supplier.setRating(newTotal / (supplier.getTotalOrders() + 1));
        }

        supplier.setTotalOrders(supplier.getTotalOrders() + 1);
        supplierRepository.save(supplier);

        log.info("Supplier rating updated. New average: {}", supplier.getRating());
    }

    @Transactional
    public void deactivateSupplier(Long id) {
        log.info("Deactivating supplier with ID: {}", id);

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new SupplierNotFoundException
                		("Supplier not found with ID: " + id));

        supplier.setIsActive(false);
        supplierRepository.save(supplier);

        log.info("Supplier deactivated successfully with ID: {}", id);
    }

    @Transactional
    public void deleteSupplier(Long id) {
        log.info("Deleting supplier with ID: {}", id);

        if (!supplierRepository.existsById(id)) {
            throw new SupplierNotFoundException
            ("Supplier not found with ID: " + id);
        }

        supplierRepository.deleteById(id);
        log.info("Supplier deleted successfully with ID: {}", id);
    }

    // Helper methods
    private Supplier mapToEntity(SupplierRequestDTO dto) {
        return Supplier.builder()
                .name(dto.getName())
                .contactPerson(dto.getContactPerson())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .city(dto.getCity())
                .country(dto.getCountry())
                .taxId(dto.getTaxId())
                .paymentTerms(dto.getPaymentTerms())
                .leadTimeDays(dto.getLeadTimeDays())
                .isActive(true)
                .rating(0.0)
                .totalOrders(0)
                .build();
    }

    private void updateEntityFromDTO(Supplier supplier, SupplierRequestDTO dto) {
        supplier.setName(dto.getName());
        supplier.setContactPerson(dto.getContactPerson());
        supplier.setEmail(dto.getEmail());
        supplier.setPhone(dto.getPhone());
        supplier.setAddress(dto.getAddress());
        supplier.setCity(dto.getCity());
        supplier.setCountry(dto.getCountry());
        supplier.setTaxId(dto.getTaxId());
        supplier.setPaymentTerms(dto.getPaymentTerms());
        supplier.setLeadTimeDays(dto.getLeadTimeDays());
    }

    private SupplierResponseDTO mapToDTO(Supplier supplier) {
        return SupplierResponseDTO.builder()
                .supplierId(supplier.getSupplierId())
                .name(supplier.getName())
                .contactPerson(supplier.getContactPerson())
                .email(supplier.getEmail())
                .phone(supplier.getPhone())
                .address(supplier.getAddress())
                .city(supplier.getCity())
                .country(supplier.getCountry())
                .taxId(supplier.getTaxId())
                .paymentTerms(supplier.getPaymentTerms())
                .leadTimeDays(supplier.getLeadTimeDays())
                .rating(supplier.getRating())
                .totalOrders(supplier.getTotalOrders())
                .isActive(supplier.getIsActive())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }
}