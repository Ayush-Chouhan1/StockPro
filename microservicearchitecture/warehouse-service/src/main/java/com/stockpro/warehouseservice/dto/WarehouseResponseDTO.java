package com.stockpro.warehouseservice.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseResponseDTO {

    private Long warehouseId;
    private String name;
    private String location;
    private String address;
    private Long managerId;
    private Integer capacity;
    private Integer usedCapacity;
    private String phone;
    private Boolean isActive;
    private LocalDateTime createdAt;
}