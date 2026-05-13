package com.stockpro.movementservice;

import com.stockpro.movementservice.dto.*;
import com.stockpro.movementservice.entity.*;
import com.stockpro.movementservice.exception.MovementNotFoundException;
import com.stockpro.movementservice.repository.StockMovementRepository;
import com.stockpro.movementservice.service.StockMovementService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {

    @Mock
    private StockMovementRepository movementRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private StockMovementService movementService;

    private StockMovement mockMovement;
    private StockMovementRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(movementService, "warehouseUrl", "http://warehouse");
        ReflectionTestUtils.setField(movementService, "reportUrl", "http://report");

        mockMovement = StockMovement.builder()
                .movementId(1L)
                .productId(1L)
                .warehouseId(1L)
                .movementType(MovementType.STOCK_IN)
                .quantity(100)
                .referenceId(1L)
                .referenceType("PO")
                .unitCost(new BigDecimal("50.00"))
                .performedBy(1L)
                .notes("Initial stock")
                .movementDate(LocalDateTime.now())
                .balanceAfter(100)
                .build();

        requestDTO = new StockMovementRequestDTO();
        requestDTO.setProductId(1L);
        requestDTO.setWarehouseId(1L);
        requestDTO.setMovementType(MovementType.STOCK_IN);
        requestDTO.setQuantity(100);
        requestDTO.setReferenceId(1L);
        requestDTO.setReferenceType("PO");
        requestDTO.setUnitCost(new BigDecimal("50.00"));
        requestDTO.setPerformedBy(1L);
        requestDTO.setNotes("Initial stock");
        requestDTO.setBalanceAfter(100);
    }

    @Test
    void recordMovement_Success() {
        when(movementRepository.save(any(StockMovement.class)))
                .thenReturn(mockMovement);

        StockMovementResponseDTO result =
                movementService.recordMovement(requestDTO);

        assertNotNull(result);
        assertEquals(MovementType.STOCK_IN, result.getMovementType());
        assertEquals(100, result.getQuantity());
        verify(movementRepository).save(any(StockMovement.class));
        verify(restTemplate).put(eq("http://warehouse/stock/warehouse/1/update"), anyMap());
        verify(restTemplate).postForObject(contains("http://report/reports/snapshot"), isNull(), eq(Map.class));
    }

    @Test
    void recordMovement_NullBalanceAfter_DoesNotSyncStockLevel() {
        requestDTO.setBalanceAfter(null);
        when(movementRepository.save(any(StockMovement.class)))
                .thenReturn(mockMovement);

        StockMovementResponseDTO result = movementService.recordMovement(requestDTO);

        assertNotNull(result);
        verify(restTemplate, never()).put(anyString(), any());
        verify(restTemplate, never()).postForObject(anyString(), any(), eq(Map.class));
    }

    @Test
    void recordMovement_WarehouseSyncFailure_ThrowsIllegalStateException() {
        doThrow(new RuntimeException("warehouse down")).when(restTemplate)
                .put(anyString(), anyMap());
        when(movementRepository.save(any(StockMovement.class)))
                .thenReturn(mockMovement);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> movementService.recordMovement(requestDTO));

        assertTrue(ex.getMessage().contains("Failed to update warehouse stock"));
    }

    @Test
    void recordMovement_ReportSyncFailure_DoesNotFailMovement() {
        when(movementRepository.save(any(StockMovement.class)))
                .thenReturn(mockMovement);
        when(restTemplate.postForObject(anyString(), isNull(), eq(Map.class)))
                .thenThrow(new RuntimeException("report down"));

        StockMovementResponseDTO result = movementService.recordMovement(requestDTO);

        assertNotNull(result);
        verify(restTemplate).put(anyString(), anyMap());
    }

    @Test
    void recordMovement_StockOut_Success() {
        requestDTO.setMovementType(MovementType.STOCK_OUT);
        requestDTO.setQuantity(20);
        requestDTO.setBalanceAfter(80);
        mockMovement.setMovementType(MovementType.STOCK_OUT);

        when(movementRepository.save(any(StockMovement.class)))
                .thenReturn(mockMovement);

        StockMovementResponseDTO result =
                movementService.recordMovement(requestDTO);

        assertNotNull(result);
        verify(movementRepository).save(any(StockMovement.class));
    }

    @Test
    void getMovementById_Success() {
        when(movementRepository.findById(1L))
                .thenReturn(Optional.of(mockMovement));

        StockMovementResponseDTO result =
                movementService.getMovementById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getMovementId());
    }

    @Test
    void getMovementById_NotFound_ThrowsException() {
        when(movementRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(MovementNotFoundException.class,
                () -> movementService.getMovementById(99L));
    }

    @Test
    void getAllMovements_ReturnsList() {
        when(movementRepository.findAll())
                .thenReturn(List.of(mockMovement));

        List<StockMovementResponseDTO> result =
                movementService.getAllMovements();

        assertEquals(1, result.size());
    }

    @Test
    void getAllMovements_EmptyList() {
        when(movementRepository.findAll()).thenReturn(List.of());

        List<StockMovementResponseDTO> result =
                movementService.getAllMovements();

        assertTrue(result.isEmpty());
    }

    @Test
    void getByProduct_ReturnsList() {
        when(movementRepository.findByProductId(1L))
                .thenReturn(List.of(mockMovement));

        List<StockMovementResponseDTO> result =
                movementService.getByProduct(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getProductId());
    }

    @Test
    void getByWarehouse_ReturnsList() {
        when(movementRepository.findByWarehouseId(1L))
                .thenReturn(List.of(mockMovement));

        List<StockMovementResponseDTO> result =
                movementService.getByWarehouse(1L);

        assertEquals(1, result.size());
    }

    @Test
    void getByType_ValidType_ReturnsList() {
        when(movementRepository.findByMovementType(MovementType.STOCK_IN))
                .thenReturn(List.of(mockMovement));

        List<StockMovementResponseDTO> result =
                movementService.getByType("STOCK_IN");

        assertEquals(1, result.size());
        assertEquals(MovementType.STOCK_IN, result.get(0).getMovementType());
    }

    @Test
    void getByType_InvalidType_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> movementService.getByType("INVALID_TYPE"));
    }

    @Test
    void getByReference_ReturnsList() {
        when(movementRepository.findByReferenceId(1L))
                .thenReturn(List.of(mockMovement));

        List<StockMovementResponseDTO> result =
                movementService.getByReference(1L);

        assertEquals(1, result.size());
    }

    @Test
    void getByPerformedBy_ReturnsList() {
        when(movementRepository.findByPerformedBy(1L))
                .thenReturn(List.of(mockMovement));

        List<StockMovementResponseDTO> result =
                movementService.getByPerformedBy(1L);

        assertEquals(1, result.size());
    }

    @Test
    void getByDateRange_ValidRange_ReturnsList() {
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();

        when(movementRepository.findByMovementDateBetween(start, end))
                .thenReturn(List.of(mockMovement));

        List<StockMovementResponseDTO> result =
                movementService.getByDateRange(start, end);

        assertEquals(1, result.size());
    }

    @Test
    void getByDateRange_InvalidRange_ThrowsException() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().minusDays(7);

        assertThrows(IllegalArgumentException.class,
                () -> movementService.getByDateRange(start, end));
    }

    @Test
    void getMovementHistory_ReturnsList() {
        when(movementRepository
                .findByProductIdAndWarehouseIdOrderByMovementDateDesc(1L, 1L))
                .thenReturn(List.of(mockMovement));

        List<StockMovementResponseDTO> result =
                movementService.getMovementHistory(1L, 1L);

        assertEquals(1, result.size());
    }

    @Test
    void getTotalStockIn_ReturnsCorrectValue() {
        when(movementRepository.getTotalStockIn(1L, 1L)).thenReturn(500);

        Integer result = movementService.getTotalStockIn(1L, 1L);

        assertEquals(500, result);
    }

    @Test
    void getTotalStockOut_ReturnsCorrectValue() {
        when(movementRepository.getTotalStockOut(1L, 1L)).thenReturn(200);

        Integer result = movementService.getTotalStockOut(1L, 1L);

        assertEquals(200, result);
    }

    @Test
    void recordMovement_WriteOff_Success() {
        requestDTO.setMovementType(MovementType.WRITE_OFF);
        requestDTO.setQuantity(5);
        requestDTO.setNotes("Damaged goods");
        mockMovement.setMovementType(MovementType.WRITE_OFF);

        when(movementRepository.save(any(StockMovement.class)))
                .thenReturn(mockMovement);

        StockMovementResponseDTO result =
                movementService.recordMovement(requestDTO);

        assertNotNull(result);
        verify(movementRepository).save(any(StockMovement.class));
    }

    @Test
    void recordMovement_Transfer_Success() {
        requestDTO.setMovementType(MovementType.TRANSFER_OUT);
        requestDTO.setReferenceType("TRANSFER");
        mockMovement.setMovementType(MovementType.TRANSFER_OUT);

        when(movementRepository.save(any(StockMovement.class)))
                .thenReturn(mockMovement);

        StockMovementResponseDTO result =
                movementService.recordMovement(requestDTO);

        assertNotNull(result);
        verify(movementRepository).save(any(StockMovement.class));
    }
}
