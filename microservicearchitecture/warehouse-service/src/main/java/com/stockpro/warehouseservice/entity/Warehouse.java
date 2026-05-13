package com.stockpro.warehouseservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "warehouses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouse {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long warehouseId;

	@Column(nullable = false, unique = true, length = 100)
	private String name;

	@Column(nullable = false, length = 200)
	private String location;

	@Column(length = 300)
	private String address;

	// ID of the manager user (from auth-service)
	private Long managerId;

	@Column(nullable = false)
	private Integer capacity;

	@Column(nullable = false)
	@Builder.Default
	private Integer usedCapacity = 0;

	@Column(length = 20)
	private String phone;

	@Column(nullable = false)
	@Builder.Default
	private Boolean isActive = true;

	@Column(updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
		if (isActive == null)
			isActive = true;
		if (usedCapacity == null)
			usedCapacity = 0;
	}
}