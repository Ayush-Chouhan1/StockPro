package com.stockpro.supplierservice.repository;

import com.stockpro.supplierservice.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findByEmail(String email);

    Optional<Supplier> findByTaxId(String taxId);

    List<Supplier> findByCity(String city);

    List<Supplier> findByCountry(String country);

    List<Supplier> findByIsActive(Boolean isActive);

    boolean existsByEmail(String email);

    boolean existsByTaxId(String taxId);

    Long countByIsActive(Boolean isActive);

    @Query("SELECT s FROM Supplier s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Supplier> searchByName(@Param("keyword") String keyword);

    @Query("SELECT s FROM Supplier s WHERE " +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.city) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.country) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Supplier> searchSuppliers(@Param("keyword") String keyword);

    // Find top-rated suppliers
    @Query("SELECT s FROM Supplier s WHERE s.isActive = true AND s.rating >= :minRating ORDER BY s.rating DESC")
    List<Supplier> findTopRatedSuppliers(@Param("minRating") Double minRating);
}