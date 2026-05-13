package com.stockpro.product_service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stockpro.product_service.dto.ProductRequestDTO;
import com.stockpro.product_service.dto.ProductResponseDTO;
import com.stockpro.product_service.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/products")
@Slf4j
@Tag(name = "Product Management", description = "APIs for managing products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping
    @Operation(summary = "Create new product")
    public ResponseEntity<ProductResponseDTO> createProduct(@Valid @RequestBody ProductRequestDTO dto) {
        log.info("Received request to create product");
        ProductResponseDTO response = productService.createProduct(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ProductResponseDTO> getProductById(@PathVariable Long id) {
        log.info("Received request to get product with ID: {}", id);
        ProductResponseDTO response = productService.getProductById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Get product by SKU")
    public ResponseEntity<ProductResponseDTO> getProductBySku(@PathVariable String sku) {
        log.info("Received request to get product with SKU: {}", sku);
        ProductResponseDTO response = productService.getProductBySku(sku);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/barcode/{barcode}")
    @Operation(summary = "Get product by barcode")
    public ResponseEntity<ProductResponseDTO> getProductByBarcode(@PathVariable String barcode) {
        log.info("Received request to get product with barcode: {}", barcode);
        ProductResponseDTO response = productService.getProductByBarcode(barcode);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all products")
    public ResponseEntity<List<ProductResponseDTO>> getAllProducts() {
        log.info("Received request to get all products");
        List<ProductResponseDTO> response = productService.getAllProducts();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    @Operation(summary = "Get all active products")
    public ResponseEntity<List<ProductResponseDTO>> getActiveProducts() {
        log.info("Received request to get active products");
        List<ProductResponseDTO> response = productService.getActiveProducts();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get products by category")
    public ResponseEntity<List<ProductResponseDTO>> getProductsByCategory(@PathVariable String category) {
        log.info("Received request to get products by category: {}", category);
        List<ProductResponseDTO> response = productService.getProductsByCategory(category);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/brand/{brand}")
    @Operation(summary = "Get products by brand")
    public ResponseEntity<List<ProductResponseDTO>> getProductsByBrand(@PathVariable String brand) {
        log.info("Received request to get products by brand: {}", brand);
        List<ProductResponseDTO> response = productService.getProductsByBrand(brand);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(summary = "Search products")
    public ResponseEntity<List<ProductResponseDTO>> searchProducts(@RequestParam String keyword) {
        log.info("Received request to search products with keyword: {}", keyword);
        List<ProductResponseDTO> response = productService.searchProducts(keyword);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product")
    public ResponseEntity<ProductResponseDTO> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDTO dto) {
        log.info("Received request to update product with ID: {}", id);
        ProductResponseDTO response = productService.updateProduct(id, dto);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate product")
    public ResponseEntity<String> deactivateProduct(@PathVariable Long id) {
        log.info("Received request to deactivate product with ID: {}", id);
        productService.deactivateProduct(id);
        return ResponseEntity.ok("Product deactivated successfully");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product")
    public ResponseEntity<String> deleteProduct(@PathVariable Long id) {
        log.info("Received request to delete product with ID: {}", id);
        productService.deleteProduct(id);
        return ResponseEntity.ok("Product deleted successfully");
    }
}