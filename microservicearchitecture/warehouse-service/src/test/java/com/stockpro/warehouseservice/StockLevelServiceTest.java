package com.stockpro.warehouseservice;

import com.stockpro.warehouseservice.dto.*;
import com.stockpro.warehouseservice.entity.Warehouse;
import com.stockpro.warehouseservice.entity.StockLevel;
import com.stockpro.warehouseservice.exception.*;
import com.stockpro.warehouseservice.rabbitmq.StockEventPublisher;
import com.stockpro.warehouseservice.repository.*;
import com.stockpro.warehouseservice.service.StockLevelService;
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
class StockLevelServiceTest {

    @Mock
    private StockLevelRepository stockLevelRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private StockEventPublisher stockEventPublisher;

    @InjectMocks
    private StockLevelService stockLevelService;

    private StockLevel mockStock;

    @BeforeEach
    void setUp() {
        mockStock = StockLevel.builder()
                .stockId(1L)
                .warehouseId(1L)
                .productId(1L)
                .quantity(100)
                .reservedQuantity(10)
                .binLocation("A1")
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    @Test
    void getStockLevel_Success() {
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(mockStock));

        StockLevelResponseDTO result = stockLevelService.getStockLevel(1L, 1L);

        assertNotNull(result);
        assertEquals(100, result.getQuantity());
        assertEquals(10, result.getReservedQuantity());
        assertEquals(90, result.getAvailableQuantity());
    }

    @Test
    void getStockLevel_NotFound_ThrowsException() {
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 99L))
                .thenReturn(Optional.empty());

        assertThrows(WarehouseNotFoundException.class,
                () -> stockLevelService.getStockLevel(1L, 99L));
    }

    @Test
    void getStockByWarehouse_ReturnsList() {
        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(stockLevelRepository.findByWarehouseId(1L))
                .thenReturn(List.of(mockStock));

        List<StockLevelResponseDTO> result =
                stockLevelService.getStockByWarehouse(1L);

        assertEquals(1, result.size());
    }

    @Test
    void getStockByWarehouse_WarehouseNotFound_ThrowsException() {
        when(warehouseRepository.existsById(99L)).thenReturn(false);

        assertThrows(WarehouseNotFoundException.class,
                () -> stockLevelService.getStockByWarehouse(99L));
    }

    @Test
    void getStockByProduct_ReturnsList() {
        when(stockLevelRepository.findByProductId(1L))
                .thenReturn(List.of(mockStock));

        List<StockLevelResponseDTO> result =
                stockLevelService.getStockByProduct(1L);

        assertEquals(1, result.size());
    }

    @Test
    void updateStock_ExistingStock_UpdatesQuantity() {
        StockUpdateDTO dto = new StockUpdateDTO();
        dto.setProductId(1L);
        dto.setQuantity(200);
        dto.setBinLocation("B2");

        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(mockStock));
        when(stockLevelRepository.save(any(StockLevel.class))).thenReturn(mockStock);

        StockLevelResponseDTO result = stockLevelService.updateStock(1L, dto);

        assertNotNull(result);
        verify(stockLevelRepository).save(any(StockLevel.class));
    }

    @Test
    void updateStock_NewStock_CreatesEntry() {
        StockUpdateDTO dto = new StockUpdateDTO();
        dto.setProductId(2L);
        dto.setQuantity(50);

        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 2L))
                .thenReturn(Optional.empty());
        when(stockLevelRepository.save(any(StockLevel.class))).thenReturn(mockStock);

        StockLevelResponseDTO result = stockLevelService.updateStock(1L, dto);

        assertNotNull(result);
        verify(stockLevelRepository).save(any(StockLevel.class));
    }

    @Test
    void updateStock_QuantityAtReorderLevel_PublishesLowStockEvent() {
        StockUpdateDTO dto = new StockUpdateDTO();
        dto.setProductId(1L);
        dto.setQuantity(5);
        dto.setReorderLevel(5);

        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(mockStock));
        when(stockLevelRepository.save(any(StockLevel.class))).thenReturn(mockStock);

        stockLevelService.updateStock(1L, dto);

        verify(stockEventPublisher).publishLowStockEvent(1L, 1L, 5, 5);
    }

    @Test
    void updateStock_QuantityAboveMaxStock_PublishesOverstockEvent() {
        StockUpdateDTO dto = new StockUpdateDTO();
        dto.setProductId(1L);
        dto.setQuantity(150);
        dto.setMaxStockLevel(100);

        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(mockStock));
        when(stockLevelRepository.save(any(StockLevel.class))).thenReturn(mockStock);

        stockLevelService.updateStock(1L, dto);

        verify(stockEventPublisher).publishOverstockEvent(1L, 1L, 150, 100);
    }

    @Test
    void updateStock_EventPublisherFailure_DoesNotFailUpdate() {
        StockUpdateDTO dto = new StockUpdateDTO();
        dto.setProductId(1L);
        dto.setQuantity(5);
        dto.setReorderLevel(10);

        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(mockStock));
        when(stockLevelRepository.save(any(StockLevel.class))).thenReturn(mockStock);
        doThrow(new RuntimeException("rabbit down")).when(stockEventPublisher)
                .publishLowStockEvent(anyLong(), anyLong(), anyInt(), anyInt());

        StockLevelResponseDTO result = stockLevelService.updateStock(1L, dto);

        assertNotNull(result);
    }

    @Test
    void updateStock_RecalculatesWarehouseUsage() {
        StockUpdateDTO dto = new StockUpdateDTO();
        dto.setProductId(1L);
        dto.setQuantity(200);

        Warehouse warehouse = Warehouse.builder()
                .warehouseId(1L)
                .usedCapacity(0)
                .build();
        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(mockStock));
        when(stockLevelRepository.save(any(StockLevel.class))).thenReturn(mockStock);
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(stockLevelRepository.sumQuantityByWarehouseId(1L)).thenReturn(225L);

        stockLevelService.updateStock(1L, dto);

        verify(warehouseRepository).save(argThat(w -> w.getUsedCapacity() == 225));
    }

    @Test
    void receiveStock_ExistingStock_IncrementsQuantity() {
        StockReceiveDTO dto = new StockReceiveDTO();
        dto.setProductId(1L);
        dto.setQuantity(25);

        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(mockStock));
        when(stockLevelRepository.save(any(StockLevel.class))).thenReturn(mockStock);

        StockLevelResponseDTO result = stockLevelService.receiveStock(1L, dto);

        assertEquals(125, result.getQuantity());
        verify(stockLevelRepository).save(
                argThat(s -> s.getQuantity() == 125));
    }

    @Test
    void receiveStock_NewStock_CreatesEntry() {
        StockReceiveDTO dto = new StockReceiveDTO();
        dto.setProductId(2L);
        dto.setQuantity(30);
        dto.setBinLocation("C3");

        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 2L))
                .thenReturn(Optional.empty());
        when(stockLevelRepository.save(any(StockLevel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockLevelResponseDTO result = stockLevelService.receiveStock(1L, dto);

        assertEquals(30, result.getQuantity());
        assertEquals("C3", result.getBinLocation());
    }

    @Test
    void reserveStock_Success() {
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(mockStock));
        when(stockLevelRepository.save(any(StockLevel.class))).thenReturn(mockStock);

        assertDoesNotThrow(() ->
                stockLevelService.reserveStock(1L, 1L, 50));

        verify(stockLevelRepository).save(
                argThat(s -> s.getReservedQuantity() == 60));
    }

    @Test
    void reserveStock_InsufficientStock_ThrowsException() {
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(mockStock));

        // Available is 90 (100 - 10), requesting 95
        assertThrows(InsufficientStockException.class,
                () -> stockLevelService.reserveStock(1L, 1L, 95));
    }

    @Test
    void releaseReservation_Success() {
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(mockStock));
        when(stockLevelRepository.save(any(StockLevel.class))).thenReturn(mockStock);

        assertDoesNotThrow(() ->
                stockLevelService.releaseReservation(1L, 1L, 5));

        verify(stockLevelRepository).save(
                argThat(s -> s.getReservedQuantity() == 5));
    }

    @Test
    void releaseReservation_MoreThanReserved_ClampsToZero() {
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(mockStock));

        stockLevelService.releaseReservation(1L, 1L, 99);

        verify(stockLevelRepository).save(argThat(s -> s.getReservedQuantity() == 0));
    }

    @Test
    void reserveStock_NotFound_ThrowsException() {
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(WarehouseNotFoundException.class,
                () -> stockLevelService.reserveStock(1L, 1L, 1));
    }

    @Test
    void releaseReservation_NotFound_ThrowsException() {
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(WarehouseNotFoundException.class,
                () -> stockLevelService.releaseReservation(1L, 1L, 1));
    }

    @Test
    void transferStock_Success() {
        StockTransferDTO dto = new StockTransferDTO();
        dto.setFromWarehouseId(1L);
        dto.setToWarehouseId(2L);
        dto.setProductId(1L);
        dto.setQuantity(30);
        dto.setReason("Rebalancing");

        StockLevel destination = StockLevel.builder()
                .warehouseId(2L).productId(1L)
                .quantity(50).reservedQuantity(0).build();

        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(warehouseRepository.existsById(2L)).thenReturn(true);
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(mockStock));
        when(stockLevelRepository.findByWarehouseIdAndProductId(2L, 1L))
                .thenReturn(Optional.of(destination));
        when(stockLevelRepository.save(any(StockLevel.class))).thenReturn(mockStock);

        assertDoesNotThrow(() -> stockLevelService.transferStock(dto));
        verify(stockLevelRepository, times(2)).save(any(StockLevel.class));
    }

    @Test
    void transferStock_SameWarehouse_ThrowsException() {
        StockTransferDTO dto = new StockTransferDTO();
        dto.setFromWarehouseId(1L);
        dto.setToWarehouseId(1L);
        dto.setProductId(1L);
        dto.setQuantity(10);
        dto.setReason("Test");

        assertThrows(IllegalArgumentException.class,
                () -> stockLevelService.transferStock(dto));
    }

    @Test
    void transferStock_InsufficientStock_ThrowsException() {
        StockTransferDTO dto = new StockTransferDTO();
        dto.setFromWarehouseId(1L);
        dto.setToWarehouseId(2L);
        dto.setProductId(1L);
        dto.setQuantity(200);
        dto.setReason("Test");

        when(warehouseRepository.existsById(1L)).thenReturn(true);
        when(warehouseRepository.existsById(2L)).thenReturn(true);
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 1L))
                .thenReturn(Optional.of(mockStock));

        assertThrows(InsufficientStockException.class,
                () -> stockLevelService.transferStock(dto));
    }

    @Test
    void getLowStockItems_ReturnsList() {
        when(stockLevelRepository.findLowStockItems(10))
                .thenReturn(List.of(mockStock));

        List<StockLevelResponseDTO> result =
                stockLevelService.getLowStockItems(10);

        assertEquals(1, result.size());
    }
}
