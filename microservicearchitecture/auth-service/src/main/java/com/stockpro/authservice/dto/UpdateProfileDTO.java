package com.stockpro.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProfileDTO {

    @NotBlank(message = "Name cannot be empty")
    private String name;

    private String phone;

    private String department;
}