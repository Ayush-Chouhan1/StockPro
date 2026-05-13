package com.stockpro.product_service.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stockpro.product_service.dto.ProductRequestDTO;
import com.stockpro.product_service.dto.ProductResponseDTO;
import com.stockpro.product_service.entity.Product;
import com.stockpro.product_service.repository.ProductRepository;
import com.stockpro.product_service.exception.DuplicateSkuException;
import com.stockpro.product_service.exception.ProductNotFoundException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO dto) {
        log.info("Creating product with SKU: {}", dto.getSku());

        if (productRepository.existsBySku(dto.getSku())) {
            throw new DuplicateSkuException("Product with SKU " + dto.getSku() + " already exists");
        }

        if (dto.getBarcode() != null && productRepository.existsByBarcode(dto.getBarcode())) {
            throw new DuplicateSkuException("Product with barcode " + dto.getBarcode() + " already exists");
        }

        Product product = mapToEntity(dto);
        Product savedProduct = productRepository.save(product);

        log.info("Product created successfully with ID: {}", savedProduct.getProductId());
        return mapToDTO(savedProduct);
    }

    public ProductResponseDTO getProductById(Long id) {
        log.info("Fetching product with ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + id));
        return mapToDTO(product);
    }

    public ProductResponseDTO getProductBySku(String sku) {
        log.info("Fetching product with SKU: {}", sku);
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with SKU: " + sku));
        return mapToDTO(product);
    }

    public ProductResponseDTO getProductByBarcode(String barcode) {
        log.info("Fetching product with barcode: {}", barcode);
        Product product = productRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with barcode: " + barcode));
        return mapToDTO(product);
    }

    public List<ProductResponseDTO> getAllProducts() {
        log.info("Fetching all products");
        return productRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<ProductResponseDTO> getActiveProducts() {
        log.info("Fetching active products");
        return productRepository.findByIsActive(true)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<ProductResponseDTO> getProductsByCategory(String category) {
        log.info("Fetching products by category: {}", category);
        return productRepository.findByCategory(category)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<ProductResponseDTO> getProductsByBrand(String brand) {
        log.info("Fetching products by brand: {}", brand);
        return productRepository.findByBrand(brand)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<ProductResponseDTO> searchProducts(String keyword) {
        log.info("Searching products with keyword: {}", keyword);
        return productRepository.searchProducts(keyword)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO dto) {
        log.info("Updating product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + id));

        // Check SKU uniqueness if changed
        if (!product.getSku().equals(dto.getSku()) && productRepository.existsBySku(dto.getSku())) {
            throw new DuplicateSkuException("Product with SKU " + dto.getSku() + " already exists");
        }

        // Check barcode uniqueness if changed
        if (dto.getBarcode() != null && 
            !dto.getBarcode().equals(product.getBarcode()) && 
            productRepository.existsByBarcode(dto.getBarcode())) {
            throw new DuplicateSkuException("Product with barcode " + dto.getBarcode() + " already exists");
        }

        updateEntityFromDTO(product, dto);
        Product updatedProduct = productRepository.save(product);

        log.info("Product updated successfully with ID: {}", id);
        return mapToDTO(updatedProduct);
    }

    @Transactional
    public void deactivateProduct(Long id) {
        log.info("Deactivating product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + id));

        product.setIsActive(false);
        productRepository.save(product);

        log.info("Product deactivated successfully with ID: {}", id);
    }

    @Transactional
    public void deleteProduct(Long id) {
        log.info("Deleting product with ID: {}", id);

        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException("Product not found with ID: " + id);
        }

        productRepository.deleteById(id);
        log.info("Product deleted successfully with ID: {}", id);
    }

    // Helper methods
    private Product mapToEntity(ProductRequestDTO dto) {
        return Product.builder()
                .sku(dto.getSku())
                .name(dto.getName())
                .description(dto.getDescription())
                .category(dto.getCategory())
                .brand(dto.getBrand())
                .unitOfMeasure(dto.getUnitOfMeasure())
                .costPrice(dto.getCostPrice())
                .sellingPrice(dto.getSellingPrice())
                .reorderLevel(dto.getReorderLevel())
                .maxStockLevel(dto.getMaxStockLevel())
                .leadTimeDays(dto.getLeadTimeDays())
                .imageUrl(dto.getImageUrl())
                .barcode(dto.getBarcode())
                .isActive(true)
                .build();
    }

    private void updateEntityFromDTO(Product product, ProductRequestDTO dto) {
        product.setSku(dto.getSku());
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setCategory(dto.getCategory());
        product.setBrand(dto.getBrand());
        product.setUnitOfMeasure(dto.getUnitOfMeasure());
        product.setCostPrice(dto.getCostPrice());
        product.setSellingPrice(dto.getSellingPrice());
        product.setReorderLevel(dto.getReorderLevel());
        product.setMaxStockLevel(dto.getMaxStockLevel());
        product.setLeadTimeDays(dto.getLeadTimeDays());
        product.setImageUrl(dto.getImageUrl());
        product.setBarcode(dto.getBarcode());
    }

    private ProductResponseDTO mapToDTO(Product product) {
        return ProductResponseDTO.builder()
                .productId(product.getProductId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .brand(product.getBrand())
                .unitOfMeasure(product.getUnitOfMeasure())
                .costPrice(product.getCostPrice())
                .sellingPrice(product.getSellingPrice())
                .reorderLevel(product.getReorderLevel())
                .maxStockLevel(product.getMaxStockLevel())
                .leadTimeDays(product.getLeadTimeDays())
                .imageUrl(product.getImageUrl())
                .barcode(product.getBarcode())
                .isActive(product.getIsActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}