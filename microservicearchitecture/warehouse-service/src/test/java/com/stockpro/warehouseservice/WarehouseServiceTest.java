package com.stockpro.warehouseservice;

import com.stockpro.warehouseservice.dto.*;
import com.stockpro.warehouseservice.entity.Warehouse;
import com.stockpro.warehouseservice.exception.WarehouseNotFoundException;
import com.stockpro.warehouseservice.repository.WarehouseRepository;
import com.stockpro.warehouseservice.service.WarehouseService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private WarehouseService warehouseService;

    private Warehouse mockWarehouse;
    private WarehouseRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        mockWarehouse = Warehouse.builder()
                .warehouseId(1L)
                .name("Main Warehouse")
                .location("Mumbai")
                .address("123 Main St")
                .managerId(1L)
                .capacity(1000)
                .usedCapacity(0)
                .phone("9999999999")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        requestDTO = new WarehouseRequestDTO();
        requestDTO.setName("Main Warehouse");
        requestDTO.setLocation("Mumbai");
        requestDTO.setAddress("123 Main St");
        requestDTO.setManagerId(1L);
        requestDTO.setCapacity(1000);
        requestDTO.setPhone("9999999999");
    }

    @Test
    void createWarehouse_Success() {
        when(warehouseRepository.existsByName("Main Warehouse")).thenReturn(false);
        when(warehouseRepository.save(any(Warehouse.class))).thenReturn(mockWarehouse);

        WarehouseResponseDTO result = warehouseService.createWarehouse(requestDTO);

        assertNotNull(result);
        assertEquals("Main Warehouse", result.getName());
        assertEquals("Mumbai", result.getLocation());
        verify(warehouseRepository).save(any(Warehouse.class));
    }

    @Test
    void createWarehouse_DuplicateName_ThrowsException() {
        when(warehouseRepository.existsByName("Main Warehouse")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> warehouseService.createWarehouse(requestDTO));
        verify(warehouseRepository, never()).save(any());
    }

    @Test
    void getWarehouseById_Success() {
        when(warehouseRepository.findById(1L))
                .thenReturn(Optional.of(mockWarehouse));

        WarehouseResponseDTO result = warehouseService.getWarehouseById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getWarehouseId());
    }

    @Test
    void getWarehouseById_NotFound_ThrowsException() {
        when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(WarehouseNotFoundException.class,
                () -> warehouseService.getWarehouseById(99L));
    }

    @Test
    void getAllWarehouses_ReturnsList() {
        when(warehouseRepository.findAll()).thenReturn(List.of(mockWarehouse));

        List<WarehouseResponseDTO> result = warehouseService.getAllWarehouses();

        assertEquals(1, result.size());
    }

    @Test
    void getAllWarehouses_EmptyList() {
        when(warehouseRepository.findAll()).thenReturn(List.of());

        List<WarehouseResponseDTO> result = warehouseService.getAllWarehouses();

        assertTrue(result.isEmpty());
    }

    @Test
    void getActiveWarehouses_ReturnsOnlyActive() {
        when(warehouseRepository.findByIsActive(true))
                .thenReturn(List.of(mockWarehouse));

        List<WarehouseResponseDTO> result = warehouseService.getActiveWarehouses();

        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsActive());
    }

    @Test
    void getWarehousesByManager_ReturnsList() {
        when(warehouseRepository.findByManagerId(1L))
                .thenReturn(List.of(mockWarehouse));

        List<WarehouseResponseDTO> result =
                warehouseService.getWarehousesByManager(1L);

        assertEquals(1, result.size());
    }

    @Test
    void updateWarehouse_Success() {
        when(warehouseRepository.findById(1L))
                .thenReturn(Optional.of(mockWarehouse));
        when(warehouseRepository.save(any(Warehouse.class))).thenReturn(mockWarehouse);

        WarehouseResponseDTO result =
                warehouseService.updateWarehouse(1L, requestDTO);

        assertNotNull(result);
        verify(warehouseRepository).save(any(Warehouse.class));
    }

    @Test
    void updateWarehouse_NotFound_ThrowsException() {
        when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(WarehouseNotFoundException.class,
                () -> warehouseService.updateWarehouse(99L, requestDTO));
    }

    @Test
    void updateWarehouse_DuplicateName_ThrowsException() {
        Warehouse existing = Warehouse.builder()
                .warehouseId(1L).name("Old Name").build();
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(warehouseRepository.existsByName("Main Warehouse")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> warehouseService.updateWarehouse(1L, requestDTO));
    }

    @Test
    void deactivateWarehouse_Success() {
        when(warehouseRepository.findById(1L))
                .thenReturn(Optional.of(mockWarehouse));
        when(warehouseRepository.save(any(Warehouse.class))).thenReturn(mockWarehouse);

        assertDoesNotThrow(() -> warehouseService.deactivateWarehouse(1L));
        verify(warehouseRepository).save(argThat(w -> !w.getIsActive()));
    }

    @Test
    void deactivateWarehouse_NotFound_ThrowsException() {
        when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(WarehouseNotFoundException.class,
                () -> warehouseService.deactivateWarehouse(99L));
    }

    @Test
    void assignManager_Success() {
        when(warehouseRepository.findById(1L))
                .thenReturn(Optional.of(mockWarehouse));
        when(warehouseRepository.save(any(Warehouse.class))).thenReturn(mockWarehouse);

        assertDoesNotThrow(() -> warehouseService.assignManager(1L, 2L));
        verify(warehouseRepository).save(argThat(w -> w.getManagerId().equals(2L)));
    }
}
