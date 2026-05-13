package com.stockpro.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleAuthRequestDTO {
    @NotBlank(message = "Google ID token is required")
    private String idToken;
}
