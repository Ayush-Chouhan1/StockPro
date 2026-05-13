package com.stockpro.supplierservice;

import com.stockpro.supplierservice.dto.SupplierRequestDTO;
import com.stockpro.supplierservice.dto.SupplierResponseDTO;
import com.stockpro.supplierservice.entity.Supplier;
import com.stockpro.supplierservice.exception.DuplicateTaxIdException;
import com.stockpro.supplierservice.exception.SupplierNotFoundException;
import com.stockpro.supplierservice.repository.SupplierRepository;
import com.stockpro.supplierservice.service.SupplierService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private SupplierService supplierService;

    private SupplierRequestDTO validSupplierDTO;
    private Supplier validSupplier;

    @BeforeEach
    void setUp() {
        validSupplierDTO = new SupplierRequestDTO();
        validSupplierDTO.setName("TechWorld Suppliers");
        validSupplierDTO.setContactPerson("Raj Kumar");
        validSupplierDTO.setEmail("raj@techworld.com");
        validSupplierDTO.setPhone("+919876543210");
        validSupplierDTO.setAddress("123 MG Road");
        validSupplierDTO.setCity("Mumbai");
        validSupplierDTO.setCountry("India");
        validSupplierDTO.setTaxId("GSTIN123456");
        validSupplierDTO.setPaymentTerms("NET-30");
        validSupplierDTO.setLeadTimeDays(7);

        validSupplier = Supplier.builder()
                .supplierId(1L)
                .name("TechWorld Suppliers")
                .contactPerson("Raj Kumar")
                .email("raj@techworld.com")
                .phone("+919876543210")
                .address("123 MG Road")
                .city("Mumbai")
                .country("India")
                .taxId("GSTIN123456")
                .paymentTerms("NET-30")
                .leadTimeDays(7)
                .isActive(true)
                .rating(4.5)
                .totalOrders(10)
                .build();
    }

    @Test
    void createSupplier_ValidSupplier_ReturnsSupplierResponse() {
        when(supplierRepository.existsByEmail(validSupplierDTO.getEmail())).thenReturn(false);
        when(supplierRepository.existsByTaxId(validSupplierDTO.getTaxId())).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenReturn(validSupplier);

        SupplierResponseDTO result = supplierService.createSupplier(validSupplierDTO);

        assertNotNull(result);
        assertEquals(validSupplierDTO.getName(), result.getName());
        verify(supplierRepository).save(any(Supplier.class));
    }

    @Test
    void createSupplier_DuplicateEmail_ThrowsException() {
        when(supplierRepository.existsByEmail(validSupplierDTO.getEmail())).thenReturn(true);

        assertThrows(DuplicateTaxIdException.class, () -> 
            supplierService.createSupplier(validSupplierDTO)
        );
    }

    @Test
    void createSupplier_DuplicateTaxId_ThrowsException() {
        when(supplierRepository.existsByEmail(validSupplierDTO.getEmail())).thenReturn(false);
        when(supplierRepository.existsByTaxId(validSupplierDTO.getTaxId())).thenReturn(true);

        assertThrows(DuplicateTaxIdException.class,
                () -> supplierService.createSupplier(validSupplierDTO));

        verify(supplierRepository, never()).save(any());
    }

    @Test
    void createSupplier_NullTaxId_DoesNotCheckTaxId() {
        validSupplierDTO.setTaxId(null);
        when(supplierRepository.existsByEmail(validSupplierDTO.getEmail())).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenReturn(validSupplier);

        SupplierResponseDTO result = supplierService.createSupplier(validSupplierDTO);

        assertNotNull(result);
        verify(supplierRepository, never()).existsByTaxId(any());
    }

    @Test
    void getSupplierById_ExistingId_ReturnsSupplier() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(validSupplier));

        SupplierResponseDTO result = supplierService.getSupplierById(1L);

        assertNotNull(result);
        assertEquals(validSupplier.getName(), result.getName());
    }

    @Test
    void getSupplierById_NonExistingId_ThrowsException() {
        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(SupplierNotFoundException.class, () -> 
            supplierService.getSupplierById(999L)
        );
    }

    @Test
    void updateRating_ValidRating_UpdatesAverage() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(validSupplier));
        when(supplierRepository.save(any(Supplier.class))).thenReturn(validSupplier);

        supplierService.updateRating(1L, 5.0);

        verify(supplierRepository).save(any(Supplier.class));
    }

    @Test
    void updateRating_FirstOrder_UsesNewRating() {
        validSupplier.setRating(0.0);
        validSupplier.setTotalOrders(0);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(validSupplier));

        supplierService.updateRating(1L, 4.0);

        assertEquals(4.0, validSupplier.getRating());
        assertEquals(1, validSupplier.getTotalOrders());
        verify(supplierRepository).save(validSupplier);
    }

    @Test
    void updateRating_NotFound_ThrowsException() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SupplierNotFoundException.class,
                () -> supplierService.updateRating(99L, 5.0));
    }

    @Test
    void getAllSuppliers_ReturnsList() {
        when(supplierRepository.findAll()).thenReturn(List.of(validSupplier));

        List<SupplierResponseDTO> result = supplierService.getAllSuppliers();

        assertEquals(1, result.size());
        assertEquals("TechWorld Suppliers", result.get(0).getName());
    }

    @Test
    void getActiveSuppliers_ReturnsList() {
        when(supplierRepository.findByIsActive(true)).thenReturn(List.of(validSupplier));

        List<SupplierResponseDTO> result = supplierService.getActiveSuppliers();

        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsActive());
    }

    @Test
    void getSuppliersByCity_ReturnsList() {
        when(supplierRepository.findByCity("Mumbai")).thenReturn(List.of(validSupplier));

        List<SupplierResponseDTO> result = supplierService.getSuppliersByCity("Mumbai");

        assertEquals(1, result.size());
        assertEquals("Mumbai", result.get(0).getCity());
    }

    @Test
    void getSuppliersByCountry_ReturnsList() {
        when(supplierRepository.findByCountry("India")).thenReturn(List.of(validSupplier));

        List<SupplierResponseDTO> result = supplierService.getSuppliersByCountry("India");

        assertEquals(1, result.size());
        assertEquals("India", result.get(0).getCountry());
    }

    @Test
    void searchSuppliers_ReturnsList() {
        when(supplierRepository.searchSuppliers("tech")).thenReturn(List.of(validSupplier));

        List<SupplierResponseDTO> result = supplierService.searchSuppliers("tech");

        assertEquals(1, result.size());
    }

    @Test
    void getTopRatedSuppliers_ReturnsList() {
        when(supplierRepository.findTopRatedSuppliers(4.0)).thenReturn(List.of(validSupplier));

        List<SupplierResponseDTO> result = supplierService.getTopRatedSuppliers(4.0);

        assertEquals(1, result.size());
        assertEquals(4.5, result.get(0).getRating());
    }

    @Test
    void updateSupplier_Success() {
        SupplierRequestDTO updated = new SupplierRequestDTO();
        updated.setName("TechWorld Updated");
        updated.setContactPerson("Asha");
        updated.setEmail("updated@techworld.com");
        updated.setPhone("1111111111");
        updated.setAddress("456 Park Street");
        updated.setCity("Pune");
        updated.setCountry("India");
        updated.setTaxId("GSTIN999");
        updated.setPaymentTerms("NET-15");
        updated.setLeadTimeDays(5);

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(validSupplier));
        when(supplierRepository.existsByEmail("updated@techworld.com")).thenReturn(false);
        when(supplierRepository.existsByTaxId("GSTIN999")).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SupplierResponseDTO result = supplierService.updateSupplier(1L, updated);

        assertEquals("TechWorld Updated", result.getName());
        assertEquals("Pune", result.getCity());
        verify(supplierRepository).save(validSupplier);
    }

    @Test
    void updateSupplier_NotFound_ThrowsException() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SupplierNotFoundException.class,
                () -> supplierService.updateSupplier(99L, validSupplierDTO));
    }

    @Test
    void updateSupplier_DuplicateEmail_ThrowsException() {
        validSupplierDTO.setEmail("new@techworld.com");
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(validSupplier));
        when(supplierRepository.existsByEmail("new@techworld.com")).thenReturn(true);

        assertThrows(DuplicateTaxIdException.class,
                () -> supplierService.updateSupplier(1L, validSupplierDTO));
    }

    @Test
    void updateSupplier_DuplicateTaxId_ThrowsException() {
        validSupplierDTO.setTaxId("GSTIN999");
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(validSupplier));
        when(supplierRepository.existsByTaxId("GSTIN999")).thenReturn(true);

        assertThrows(DuplicateTaxIdException.class,
                () -> supplierService.updateSupplier(1L, validSupplierDTO));
    }

    @Test
    void deactivateSupplier_ExistingId_Success() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(validSupplier));
        when(supplierRepository.save(any(Supplier.class))).thenReturn(validSupplier);

        supplierService.deactivateSupplier(1L);

        assertFalse(validSupplier.getIsActive());
        verify(supplierRepository).save(validSupplier);
    }

    @Test
    void deactivateSupplier_NotFound_ThrowsException() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SupplierNotFoundException.class,
                () -> supplierService.deactivateSupplier(99L));
    }

    @Test
    void deleteSupplier_Success() {
        when(supplierRepository.existsById(1L)).thenReturn(true);

        supplierService.deleteSupplier(1L);

        verify(supplierRepository).deleteById(1L);
    }

    @Test
    void deleteSupplier_NotFound_ThrowsException() {
        when(supplierRepository.existsById(99L)).thenReturn(false);

        assertThrows(SupplierNotFoundException.class,
                () -> supplierService.deleteSupplier(99L));
        verify(supplierRepository, never()).deleteById(anyLong());
    }
}
