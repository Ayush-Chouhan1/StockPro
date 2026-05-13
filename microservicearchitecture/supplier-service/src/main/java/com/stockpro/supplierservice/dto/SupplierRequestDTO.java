package com.stockpro.supplierservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SupplierRequestDTO {

    @NotBlank(message = "Supplier name is required")
    @Size(max = 200, message = "Name must be less than 200 characters")
    private String name;

    @Size(max = 200, message = "Contact person name must be less than 200 characters")
    private String contactPerson;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Phone number must be 10-15 digits")
    private String phone;

    @Size(max = 500, message = "Address must be less than 500 characters")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Country is required")
    private String country;

    @Size(max = 50, message = "Tax ID must be less than 50 characters")
    private String taxId;

    @Pattern(regexp = "^(NET-15|NET-30|NET-45|NET-60|NET-90|IMMEDIATE|COD)$", 
             message = "Payment terms must be NET-15, NET-30, NET-45, NET-60, NET-90, IMMEDIATE, or COD")
    private String paymentTerms;

    @NotNull(message = "Lead time is required")
    @Min(value = 1, message = "Lead time must be at least 1 day")
    @Max(value = 365, message = "Lead time cannot exceed 365 days")
    private Integer leadTimeDays;
}