package com.stockpro.purchaseservice;

import com.stockpro.purchaseservice.dto.*;
import com.stockpro.purchaseservice.entity.*;
import com.stockpro.purchaseservice.exception.*;
import com.stockpro.purchaseservice.rabbitmq.POEventPublisher;
import com.stockpro.purchaseservice.repository.*;
import com.stockpro.purchaseservice.service.PurchaseOrderService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository poRepository;

    @Mock
    private POLineItemRepository lineItemRepository;

    @Mock
    private POEventPublisher poEventPublisher;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PurchaseOrderService poService;

    private PurchaseOrder mockPO;
    private PurchaseOrderRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        POLineItem lineItem = POLineItem.builder()
                .lineItemId(1L)
                .productId(1L)
                .quantity(10)
                .unitCost(new BigDecimal("100.00"))
                .totalCost(new BigDecimal("1000.00"))
                .receivedQty(0)
                .build();

        mockPO = PurchaseOrder.builder()
                .poId(1L)
                .supplierId(1L)
                .warehouseId(1L)
                .createdById(1L)
                .status(POStatus.DRAFT)
                .totalAmount(new BigDecimal("1000.00"))
                .orderDate(LocalDate.now())
                .expectedDate(LocalDate.now().plusDays(7))
                .lineItems(new ArrayList<>(List.of(lineItem)))
                .build();

        lineItem.setPurchaseOrder(mockPO);

        POLineItemDTO lineItemDTO = new POLineItemDTO();
        lineItemDTO.setProductId(1L);
        lineItemDTO.setQuantity(10);
        lineItemDTO.setUnitCost(new BigDecimal("100.00"));

        requestDTO = new PurchaseOrderRequestDTO();
        requestDTO.setSupplierId(1L);
        requestDTO.setWarehouseId(1L);
        requestDTO.setCreatedById(1L);
        requestDTO.setLineItems(List.of(lineItemDTO));
        requestDTO.setExpectedDate(LocalDate.now().plusDays(7));
    }

    @Test
    void createPO_Success() {
        when(poRepository.save(any(PurchaseOrder.class))).thenReturn(mockPO);

        PurchaseOrderResponseDTO result = poService.createPO(requestDTO);

        assertNotNull(result);
        assertEquals(POStatus.DRAFT, result.getStatus());
        verify(poRepository).save(any(PurchaseOrder.class));
    }

    @Test
    void getPOById_Success() {
        when(poRepository.findById(1L)).thenReturn(Optional.of(mockPO));

        PurchaseOrderResponseDTO result = poService.getPOById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getPoId());
    }

    @Test
    void getPOById_NotFound_ThrowsException() {
        when(poRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PurchaseOrderNotFoundException.class,
                () -> poService.getPOById(99L));
    }

    @Test
    void getAllPOs_ReturnsList() {
        when(poRepository.findAll()).thenReturn(List.of(mockPO));

        List<PurchaseOrderResponseDTO> result = poService.getAllPOs();

        assertEquals(1, result.size());
    }

    @Test
    void getPOsBySupplier_ReturnsList() {
        when(poRepository.findBySupplierId(1L)).thenReturn(List.of(mockPO));

        List<PurchaseOrderResponseDTO> result = poService.getPOsBySupplier(1L);

        assertEquals(1, result.size());
    }

    @Test
    void getPOsByWarehouse_ReturnsList() {
        when(poRepository.findByWarehouseId(1L)).thenReturn(List.of(mockPO));

        List<PurchaseOrderResponseDTO> result = poService.getPOsByWarehouse(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getWarehouseId());
    }

    @Test
    void getPOsByCreatedBy_ReturnsList() {
        when(poRepository.findByCreatedById(1L)).thenReturn(List.of(mockPO));

        List<PurchaseOrderResponseDTO> result = poService.getPOsByCreatedBy(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getCreatedById());
    }

    @Test
    void getPOsByStatus_ValidStatus_ReturnsList() {
        when(poRepository.findByStatus(POStatus.DRAFT))
                .thenReturn(List.of(mockPO));

        List<PurchaseOrderResponseDTO> result = poService.getPOsByStatus("DRAFT");

        assertEquals(1, result.size());
    }

    @Test
    void getPOsByStatus_InvalidStatus_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> poService.getPOsByStatus("INVALID_STATUS"));
    }

    @Test
    void getPOsByDateRange_ValidRange_ReturnsList() {
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();
        when(poRepository.findByOrderDateBetween(start, end))
                .thenReturn(List.of(mockPO));

        List<PurchaseOrderResponseDTO> result =
                poService.getPOsByDateRange(start, end);

        assertEquals(1, result.size());
    }

    @Test
    void getPOsByDateRange_InvalidRange_ThrowsException() {
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().minusDays(7);

        assertThrows(IllegalArgumentException.class,
                () -> poService.getPOsByDateRange(start, end));
    }

    @Test
    void getOverduePOs_ReturnsApprovedPastDuePOs() {
        when(poRepository.findByStatusAndExpectedDateBefore(eq(POStatus.APPROVED), any(LocalDate.class)))
                .thenReturn(List.of(mockPO));

        List<PurchaseOrderResponseDTO> result = poService.getOverduePOs();

        assertEquals(1, result.size());
    }

    @Test
    void submitForApproval_Success() {
        when(poRepository.findById(1L)).thenReturn(Optional.of(mockPO));
        mockPO.setStatus(POStatus.DRAFT);
        PurchaseOrder pending = PurchaseOrder.builder()
                .poId(1L).status(POStatus.PENDING)
                .lineItems(mockPO.getLineItems()).build();
        when(poRepository.save(any())).thenReturn(pending);

        PurchaseOrderResponseDTO result = poService.submitForApproval(1L);

        assertEquals(POStatus.PENDING, result.getStatus());
    }

    @Test
    void submitForApproval_NotDraft_ThrowsException() {
        mockPO.setStatus(POStatus.APPROVED);
        when(poRepository.findById(1L)).thenReturn(Optional.of(mockPO));

        assertThrows(InvalidPOStatusException.class,
                () -> poService.submitForApproval(1L));
    }

    @Test
    void approvePO_Success() {
        mockPO.setStatus(POStatus.PENDING);
        when(poRepository.findById(1L)).thenReturn(Optional.of(mockPO));
        PurchaseOrder approved = PurchaseOrder.builder()
                .poId(1L).status(POStatus.APPROVED)
                .lineItems(mockPO.getLineItems()).build();
        when(poRepository.save(any())).thenReturn(approved);

        PurchaseOrderResponseDTO result = poService.approvePO(1L);

        assertEquals(POStatus.APPROVED, result.getStatus());
        verify(poEventPublisher).publishPOApproved(any(), any(), any(), any(), any(), any());
    }

    @Test
    void approvePO_PublisherFailure_DoesNotFailApproval() {
        mockPO.setStatus(POStatus.PENDING);
        when(poRepository.findById(1L)).thenReturn(Optional.of(mockPO));
        when(poRepository.save(any())).thenReturn(mockPO);
        doThrow(new RuntimeException("rabbit down")).when(poEventPublisher)
                .publishPOApproved(any(), any(), any(), any(), any(), any());

        PurchaseOrderResponseDTO result = poService.approvePO(1L);

        assertEquals(POStatus.APPROVED, result.getStatus());
    }

    @Test
    void approvePO_NotPending_ThrowsException() {
        mockPO.setStatus(POStatus.DRAFT);
        when(poRepository.findById(1L)).thenReturn(Optional.of(mockPO));

        assertThrows(InvalidPOStatusException.class,
                () -> poService.approvePO(1L));
    }

    @Test
    void rejectPO_Success() {
        mockPO.setStatus(POStatus.PENDING);
        when(poRepository.findById(1L)).thenReturn(Optional.of(mockPO));
        when(poRepository.save(any())).thenReturn(mockPO);

        PurchaseOrderResponseDTO result = poService.rejectPO(1L, "Wrong items");

        verify(poRepository).save(any());
    }

    @Test
    void rejectPO_NotPending_ThrowsException() {
        mockPO.setStatus(POStatus.APPROVED);
        when(poRepository.findById(1L)).thenReturn(Optional.of(mockPO));

        assertThrows(InvalidPOStatusException.class,
                () -> poService.rejectPO(1L, "Wrong items"));
    }

    @Test
    void cancelPO_Success() {
        mockPO.setStatus(POStatus.APPROVED);
        when(poRepository.findById(1L)).thenReturn(Optional.of(mockPO));
        PurchaseOrder cancelled = PurchaseOrder.builder()
                .poId(1L).status(POStatus.CANCELLED)
                .lineItems(mockPO.getLineItems()).build();
        when(poRepository.save(any())).thenReturn(cancelled);

        PurchaseOrderResponseDTO result =
                poService.cancelPO(1L, "No longer needed");

        assertEquals(POStatus.CANCELLED, result.getStatus());
    }

    @Test
    void cancelPO_AlreadyCancelled_ThrowsException() {
        mockPO.setStatus(POStatus.CANCELLED);
        when(poRepository.findById(1L)).thenReturn(Optional.of(mockPO));

        assertThrows(InvalidPOStatusException.class,
                () -> poService.cancelPO(1L, "reason"));
    }

    @Test
    void receiveGoods_FullReceipt_StatusFullyReceived() {
        mockPO.setStatus(POStatus.APPROVED);
        when(poRepository.findById(1L)).thenReturn(Optional.of(mockPO));

        PurchaseOrder received = PurchaseOrder.builder()
                .poId(1L).status(POStatus.FULLY_RECEIVED)
                .lineItems(mockPO.getLineItems())
                .receivedDate(LocalDate.now()).build();
        when(poRepository.save(any())).thenReturn(received);

        GoodsReceiptDTO receipt = new GoodsReceiptDTO();
        receipt.setLineItemId(1L);
        receipt.setReceivedQty(10);

        PurchaseOrderResponseDTO result =
                poService.receiveGoods(1L, List.of(receipt));

        assertEquals(POStatus.FULLY_RECEIVED, result.getStatus());
    }

    @Test
    void receiveGoods_PartialReceipt_StatusPartiallyReceived() {
        mockPO.setStatus(POStatus.APPROVED);
        when(poRepository.findById(1L)).thenReturn(Optional.of(mockPO));
        when(poRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        GoodsReceiptDTO receipt = new GoodsReceiptDTO();
        receipt.setLineItemId(1L);
        receipt.setReceivedQty(5);

        PurchaseOrderResponseDTO result = poService.receiveGoods(1L, List.of(receipt));

        assertEquals(POStatus.PARTIALLY_RECEIVED, result.getStatus());
        assertNull(result.getReceivedDate());
    }

    @Test
    void receiveGoods_LineItemMissing_ThrowsException() {
        mockPO.setStatus(POStatus.APPROVED);
        when(poRepository.findById(1L)).thenReturn(Optional.of(mockPO));

        GoodsReceiptDTO receipt = new GoodsReceiptDTO();
        receipt.setLineItemId(99L);
        receipt.setReceivedQty(1);

        assertThrows(PurchaseOrderNotFoundException.class,
                () -> poService.receiveGoods(1L, List.of(receipt)));
    }

    @Test
    void receiveGoods_InvalidStatus_ThrowsException() {
        mockPO.setStatus(POStatus.DRAFT);
        when(poRepository.findById(1L)).thenReturn(Optional.of(mockPO));

        GoodsReceiptDTO receipt = new GoodsReceiptDTO();
        receipt.setLineItemId(1L);
        receipt.setReceivedQty(5);

        assertThrows(InvalidPOStatusException.class,
                () -> poService.receiveGoods(1L, List.of(receipt)));
    }

    @Test
    void receiveGoods_ExceedsOrderedQty_ThrowsException() {
        mockPO.setStatus(POStatus.APPROVED);
        when(poRepository.findById(1L)).thenReturn(Optional.of(mockPO));

        GoodsReceiptDTO receipt = new GoodsReceiptDTO();
        receipt.setLineItemId(1L);
        receipt.setReceivedQty(999); // ordered was 10

        assertThrows(IllegalArgumentException.class,
                () -> poService.receiveGoods(1L, List.of(receipt)));
    }

    @Test
    void updatePO_DraftStatus_Success() {
        when(poRepository.findById(1L)).thenReturn(Optional.of(mockPO));
        when(poRepository.save(any())).thenReturn(mockPO);

        PurchaseOrderResponseDTO result = poService.updatePO(1L, requestDTO);

        assertNotNull(result);
        verify(poRepository).save(any());
    }

    @Test
    void updatePO_NotDraft_ThrowsException() {
        mockPO.setStatus(POStatus.APPROVED);
        when(poRepository.findById(1L)).thenReturn(Optional.of(mockPO));

        assertThrows(InvalidPOStatusException.class,
                () -> poService.updatePO(1L, requestDTO));
    }

    @Test
    void checkOverduePOs_PublishesEventsAndContinuesOnFailure() {
        PurchaseOrder second = PurchaseOrder.builder()
                .poId(2L)
                .supplierId(2L)
                .warehouseId(2L)
                .expectedDate(LocalDate.now().minusDays(1))
                .lineItems(List.of())
                .build();
        when(poRepository.findByStatusAndExpectedDateBefore(eq(POStatus.APPROVED), any(LocalDate.class)))
                .thenReturn(List.of(mockPO, second));
        doAnswer(invocation -> {
            Long poId = invocation.getArgument(0);
            if (poId.equals(2L)) {
                throw new RuntimeException("rabbit down");
            }
            return null;
        }).when(poEventPublisher).publishPOOverdue(anyLong(), anyLong(), anyLong(), any(LocalDate.class));

        poService.checkOverduePOs();

        verify(poEventPublisher).publishPOOverdue(eq(1L), any(), any(), any());
        verify(poEventPublisher).publishPOOverdue(eq(2L), any(), any(), any());
    }
}
