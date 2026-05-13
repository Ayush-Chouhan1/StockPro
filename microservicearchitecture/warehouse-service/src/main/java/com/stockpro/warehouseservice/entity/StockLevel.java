package com.stockpro.warehouseservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_levels", uniqueConstraints = @UniqueConstraint(columnNames = { "warehouse_id", "product_id" }))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockLevel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long stockId;

	@Column(nullable = false)
	private Long warehouseId;

	// Product ID from product-service
	@Column(nullable = false)
	private Long productId;

	@Column(nullable = false)
	@Builder.Default
	private Integer quantity = 0;

	@Column(nullable = false)
	@Builder.Default
	private Integer reservedQuantity = 0;

	// bin/aisle reference
	@Column(length = 50)
	private String binLocation;

	private LocalDateTime lastUpdated;

	
	public Integer getAvailableQuantity() {
		return quantity - reservedQuantity;
	}

	@PrePersist
	@PreUpdate
	protected void onUpdate() {
		lastUpdated = LocalDateTime.now();
		if (quantity == null)
			quantity = 0;
		if (reservedQuantity == null)
			reservedQuantity = 0;
	}
}