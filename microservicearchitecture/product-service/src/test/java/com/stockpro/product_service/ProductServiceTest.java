package com.stockpro.product_service;

import com.stockpro.product_service.dto.ProductRequestDTO;
import com.stockpro.product_service.dto.ProductResponseDTO;
import com.stockpro.product_service.entity.Product;
import com.stockpro.product_service.exception.DuplicateSkuException;
import com.stockpro.product_service.exception.ProductNotFoundException;
import com.stockpro.product_service.repository.ProductRepository;
import com.stockpro.product_service.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product mockProduct;
    private ProductRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        mockProduct = Product.builder()
                .productId(1L)
                .sku("SKU-001")
                .name("Test Product")
                .description("Test Description")
                .category("Electronics")
                .brand("Dell")
                .unitOfMeasure("piece")
                .costPrice(new BigDecimal("100.00"))
                .sellingPrice(new BigDecimal("150.00"))
                .reorderLevel(5)
                .maxStockLevel(100)
                .leadTimeDays(7)
                .barcode("1234567890")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        requestDTO = new ProductRequestDTO();
        requestDTO.setSku("SKU-001");
        requestDTO.setName("Test Product");
        requestDTO.setDescription("Test Description");
        requestDTO.setCategory("Electronics");
        requestDTO.setBrand("Dell");
        requestDTO.setUnitOfMeasure("piece");
        requestDTO.setCostPrice(new BigDecimal("100.00"));
        requestDTO.setSellingPrice(new BigDecimal("150.00"));
        requestDTO.setReorderLevel(5);
        requestDTO.setMaxStockLevel(100);
        requestDTO.setLeadTimeDays(7);
        requestDTO.setBarcode("1234567890");
    }

    
    @Test
    void createProduct_Success() {
        when(productRepository.existsBySku("SKU-001")).thenReturn(false);
        when(productRepository.existsByBarcode("1234567890")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(mockProduct);

        ProductResponseDTO result = productService.createProduct(requestDTO);

        assertNotNull(result);
        assertEquals("SKU-001", result.getSku());
        assertEquals("Test Product", result.getName());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_DuplicateSku_ThrowsException() {
        when(productRepository.existsBySku("SKU-001")).thenReturn(true);

        assertThrows(DuplicateSkuException.class,
                () -> productService.createProduct(requestDTO));

        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_DuplicateBarcode_ThrowsException() {
        when(productRepository.existsBySku("SKU-001")).thenReturn(false);
        when(productRepository.existsByBarcode("1234567890")).thenReturn(true);

        assertThrows(DuplicateSkuException.class,
                () -> productService.createProduct(requestDTO));
    }

    @Test
    void createProduct_NullBarcode_DoesNotCheckBarcode() {
        requestDTO.setBarcode(null);
        when(productRepository.existsBySku("SKU-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(mockProduct);

        productService.createProduct(requestDTO);

        verify(productRepository, never()).existsByBarcode(any());
    }


    @Test
    void getProductById_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));

        ProductResponseDTO result = productService.getProductById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getProductId());
        assertEquals("SKU-001", result.getSku());
    }

    
    // Get by product ID
    @Test
    void getProductById_NotFound_ThrowsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> productService.getProductById(99L));
    }

    // Get by SKU id

    @Test
    void getProductBySku_Success() {
        when(productRepository.findBySku("SKU-001"))
                .thenReturn(Optional.of(mockProduct));

        ProductResponseDTO result = productService.getProductBySku("SKU-001");

        assertNotNull(result);
        assertEquals("SKU-001", result.getSku());
    }

    @Test
    void getProductBySku_NotFound_ThrowsException() {
        when(productRepository.findBySku("INVALID"))
                .thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> productService.getProductBySku("INVALID"));
    }

    // Get by barcode 

    @Test
    void getProductByBarcode_Success() {
        when(productRepository.findByBarcode("1234567890"))
                .thenReturn(Optional.of(mockProduct));

        ProductResponseDTO result = productService.getProductByBarcode("1234567890");

        assertNotNull(result);
        assertEquals("1234567890", result.getBarcode());
    }

    @Test
    void getProductByBarcode_NotFound_ThrowsException() {
        when(productRepository.findByBarcode("000"))
                .thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> productService.getProductByBarcode("000"));
    }

    // Get all

    @Test
    void getAllProducts_ReturnsList() {
        when(productRepository.findAll()).thenReturn(List.of(mockProduct));

        List<ProductResponseDTO> result = productService.getAllProducts();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getAllProducts_EmptyList() {
        when(productRepository.findAll()).thenReturn(List.of());

        List<ProductResponseDTO> result = productService.getAllProducts();

        assertTrue(result.isEmpty());
    }

    // Get active

    @Test
    void getActiveProducts_ReturnsOnlyActive() {
        when(productRepository.findByIsActive(true))
                .thenReturn(List.of(mockProduct));

        List<ProductResponseDTO> result = productService.getActiveProducts();

        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsActive());
    }

    // Get by category

    @Test
    void getProductsByCategory_ReturnsList() {
        when(productRepository.findByCategory("Electronics"))
                .thenReturn(List.of(mockProduct));

        List<ProductResponseDTO> result =
                productService.getProductsByCategory("Electronics");

        assertEquals(1, result.size());
        assertEquals("Electronics", result.get(0).getCategory());
    }

    // Get by brand

    @Test
    void getProductsByBrand_ReturnsList() {
        when(productRepository.findByBrand("Dell"))
                .thenReturn(List.of(mockProduct));

        List<ProductResponseDTO> result = productService.getProductsByBrand("Dell");

        assertEquals(1, result.size());
        assertEquals("Dell", result.get(0).getBrand());
    }

    //Search

    @Test
    void searchProducts_ReturnsList() {
        when(productRepository.searchProducts("laptop"))
                .thenReturn(List.of(mockProduct));

        List<ProductResponseDTO> result = productService.searchProducts("laptop");

        assertEquals(1, result.size());
    }

    @Test
    void searchProducts_NoResults_ReturnsEmpty() {
        when(productRepository.searchProducts("xyz123"))
                .thenReturn(List.of());

        List<ProductResponseDTO> result = productService.searchProducts("xyz123");

        assertTrue(result.isEmpty());
    }

    // Update

    @Test
    void updateProduct_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(productRepository.save(any(Product.class))).thenReturn(mockProduct);

        ProductResponseDTO result = productService.updateProduct(1L, requestDTO);

        assertNotNull(result);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateProduct_NotFound_ThrowsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> productService.updateProduct(99L, requestDTO));
    }

    @Test
    void updateProduct_DuplicateSku_ThrowsException() {
        Product existing = Product.builder()
                .productId(1L).sku("OLD-SKU").build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySku("SKU-001")).thenReturn(true);

        assertThrows(DuplicateSkuException.class,
                () -> productService.updateProduct(1L, requestDTO));
    }

    // Deactivate

    @Test
    void deactivateProduct_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(productRepository.save(any(Product.class))).thenReturn(mockProduct);

        assertDoesNotThrow(() -> productService.deactivateProduct(1L));
        verify(productRepository).save(argThat(p -> !p.getIsActive()));
    }

    @Test
    void deactivateProduct_NotFound_ThrowsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> productService.deactivateProduct(99L));
    }

    // Delete

    @Test
    void deleteProduct_Success() {
        when(productRepository.existsById(1L)).thenReturn(true);
        doNothing().when(productRepository).deleteById(1L);

        assertDoesNotThrow(() -> productService.deleteProduct(1L));
        verify(productRepository).deleteById(1L);
    }

    @Test
    void deleteProduct_NotFound_ThrowsException() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThrows(ProductNotFoundException.class,
                () -> productService.deleteProduct(99L));
    }
}
