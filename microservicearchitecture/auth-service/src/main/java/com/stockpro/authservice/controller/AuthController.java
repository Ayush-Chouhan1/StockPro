package com.stockpro.authservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.stockpro.authservice.dto.*;
import com.stockpro.authservice.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User registration, login, token refresh and profile management")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    // Public endpoints

    @PostMapping("/register")
    @Operation(summary = "Register a new user",
               description = "Creates a new user account. No authentication required.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User registered successfully",
                     content = @Content(schema = @Schema(implementation = RegisterResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Validation error or email already exists",
                     content = @Content),
        @ApiResponse(responseCode = "409", description = "User already exists",
                     content = @Content)
    })
    public ResponseEntity<RegisterResponseDTO> register(@Valid @RequestBody UserRequestDTO dto) {
        log.info("Register request: {}", dto.getEmail());
        return ResponseEntity.ok(authService.register(dto));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and obtain JWT tokens",
               description = "Authenticates the user and returns an access token and refresh token.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful",
                     content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials",
                     content = @Content)
    })
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        log.info("Login request: {}", dto.getEmail());
        return ResponseEntity.ok(authService.login(dto.getEmail(), dto.getPassword()));
    }

    @PostMapping("/google")
    @Operation(summary = "Login with Google",
               description = "Validates a Google ID token and returns StockPro JWT tokens.")
    public ResponseEntity<LoginResponseDTO> googleLogin(
            @Valid @RequestBody GoogleAuthRequestDTO dto) {
        return ResponseEntity.ok(authService.googleLogin(dto.getIdToken()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token",
               description = "Exchanges a valid refresh token for a new access token.")
    @ApiResponse(responseCode = "200", description = "New token issued",
                 content = @Content(schema = @Schema(implementation = LoginResponseDTO.class)))
    public ResponseEntity<LoginResponseDTO> refresh(
            @Parameter(description = "Refresh token string", required = true)
            @RequestBody String refreshToken) {
        return ResponseEntity.ok(authService.refresh(refreshToken.trim()));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset",
               description = "Sends a password reset link to the supplied email address when the account exists.")
    @ApiResponse(responseCode = "200", description = "Password reset request accepted")
    public ResponseEntity<String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO dto) {
        authService.requestPasswordReset(dto);
        return ResponseEntity.ok("If an account exists for this email, a password reset link has been sent");
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password",
               description = "Resets the password using a valid reset token sent over email.")
    @ApiResponse(responseCode = "200", description = "Password reset successfully")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO dto) {
        authService.resetPassword(dto);
        return ResponseEntity.ok("Password reset successfully");
    }

    // Authenticated endpoints

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Logout (invalidate token)",
               description = "Invalidates the current JWT access token.")
    @ApiResponse(responseCode = "200", description = "Logged out successfully")
    public ResponseEntity<String> logout(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7));
        }
        return ResponseEntity.ok("Logged out successfully");
    }

    @GetMapping("/profile")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get current user profile")
    @ApiResponse(responseCode = "200", description = "Profile retrieved",
                 content = @Content(schema = @Schema(implementation = UserResponseDTO.class)))
    public ResponseEntity<UserResponseDTO> getProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(authService.getUserProfile(auth.getName()));
    }

    @PutMapping("/profile")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update current user profile")
    @ApiResponse(responseCode = "200", description = "Profile updated",
                 content = @Content(schema = @Schema(implementation = UserResponseDTO.class)))
    public ResponseEntity<UserResponseDTO> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(authService.updateProfile(auth.getName(), dto));
    }

    @PutMapping("/password")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Change password")
    @ApiResponse(responseCode = "200", description = "Password changed successfully")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        authService.changePassword(auth.getName(), dto);
        return ResponseEntity.ok("Password changed successfully");
    }

    // Admin-only endpoints

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get all users (Admin only)")
    @ApiResponse(responseCode = "200", description = "List of all users")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create internal system account (Admin only)")
    public ResponseEntity<RegisterResponseDTO> createInternalUser(
            @Valid @RequestBody UserRequestDTO dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(authService.createInternalAccount(dto, auth.getName()));
    }

    @DeleteMapping("/user/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Deactivate a user account (Admin only)")
    @ApiResponse(responseCode = "200", description = "Account deactivated successfully")
    public ResponseEntity<String> deactivateUser(
            @Parameter(description = "User ID", required = true) @PathVariable Long id) {
        authService.deactivate(id);
        return ResponseEntity.ok("Account deactivated successfully");
    }
}
