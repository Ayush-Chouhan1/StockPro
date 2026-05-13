package com.stockpro.warehouseservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class WarehouseRequestDTO {

    @NotBlank(message = "Warehouse name is required")
    @Size(max = 100, message = "Name must be less than 100 characters")
    private String name;

    @NotBlank(message = "Location is required")
    @Size(max = 200, message = "Location must be less than 200 characters")
    private String location;

    @Size(max = 300, message = "Address must be less than 300 characters")
    private String address;

    private Long managerId;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;
}