package com.stockpro.authservice.dto;

import java.time.LocalDateTime;

import com.stockpro.authservice.entity.UserRole;

import lombok.Data;

@Data
public class UserResponseDTO {
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private UserRole role;
    private String department;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
