package com.stockpro.authservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpro.authservice.controller.AuthController;
import com.stockpro.authservice.dto.*;
import com.stockpro.authservice.entity.UserRole;
import com.stockpro.authservice.exception.GlobalExceptionHandler;
import com.stockpro.authservice.exception.UserAlreadyExistsException;
import com.stockpro.authservice.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	@Mock
	private AuthService authService;

	@InjectMocks
	private AuthController authController;

	private UserResponseDTO mockResponse;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(authController)
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
		objectMapper = new ObjectMapper();
		objectMapper.findAndRegisterModules();

		mockResponse = new UserResponseDTO();
		mockResponse.setUserId(1L);
		mockResponse.setName("Test User");
		mockResponse.setEmail("test@gmail.com");
		mockResponse.setRole(UserRole.WAREHOUSE_STAFF);
		mockResponse.setIsActive(true);
		mockResponse.setCreatedAt(LocalDateTime.now());

		UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("test@gmail.com", null,
				List.of(new SimpleGrantedAuthority("ROLE_STAFF")));
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	// Register

	@Test
	void register_Success_Returns200() throws Exception {
		UserRequestDTO dto = new UserRequestDTO();
		dto.setName("Test User");
		dto.setEmail("test@gmail.com");
		dto.setPassword("password123");
		dto.setRole(UserRole.WAREHOUSE_STAFF);

		when(authService.register(any(UserRequestDTO.class)))
				.thenReturn(new RegisterResponseDTO(1L, "User registered successfully"));

		mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto))).andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(1L))
				.andExpect(jsonPath("$.message").value("User registered successfully"));
	}

	@Test
	void register_DuplicateEmail_Returns409() throws Exception {
		UserRequestDTO dto = new UserRequestDTO();
		dto.setName("Test User");
		dto.setEmail("test@gmail.com");
		dto.setPassword("password123");
		dto.setRole(UserRole.WAREHOUSE_STAFF);

		when(authService.register(any(UserRequestDTO.class)))
				.thenThrow(new UserAlreadyExistsException("Email already exists"));

		mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto))).andExpect(status().isConflict());
	}

	@Test
	void register_MissingName_Returns400() throws Exception {
		UserRequestDTO dto = new UserRequestDTO();
		dto.setEmail("test@gmail.com");
		dto.setPassword("password123");
		dto.setRole(UserRole.WAREHOUSE_STAFF);
		// name is missing

		mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto))).andExpect(status().isBadRequest());
	}

	//Login 

	@Test
	void login_Success_ReturnsTokens() throws Exception {
		LoginRequestDTO dto = new LoginRequestDTO();
		dto.setEmail("test@gmail.com");
		dto.setPassword("password123");

		LoginResponseDTO response = new LoginResponseDTO("access-token", "refresh-token");
		when(authService.login(anyString(), anyString())).thenReturn(response);

		mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto))).andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("access-token"))
				.andExpect(jsonPath("$.refreshToken").value("refresh-token"));
	}

	@Test
	void login_InvalidCredentials_Returns400() throws Exception {
		LoginRequestDTO dto = new LoginRequestDTO();
		dto.setEmail("test@gmail.com");
		dto.setPassword("wrongpassword");

		when(authService.login(anyString(), anyString())).thenThrow(new RuntimeException("Invalid credentials"));

		mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto))).andExpect(status().isBadRequest());
	}

	//Logout

	@Test
	void logout_Success_Returns200() throws Exception {
		doNothing().when(authService).logout(anyString());

		mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer some-token")).andExpect(status().isOk())
				.andExpect(content().string("Logged out successfully"));
	}

	//Refresh

	@Test
	void refresh_Success_ReturnsNewTokens() throws Exception {
		LoginResponseDTO response = new LoginResponseDTO("new-access-token", "new-refresh-token");
		when(authService.refresh(anyString())).thenReturn(response);

		mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("my-refresh-token"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").value("new-access-token"));
	}

	@Test
	void forgotPassword_Success_ReturnsGenericMessage() throws Exception {
		ForgotPasswordRequestDTO dto = new ForgotPasswordRequestDTO();
		dto.setEmail("test@gmail.com");

		doNothing().when(authService).requestPasswordReset(any(ForgotPasswordRequestDTO.class));

		mockMvc.perform(post("/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto))).andExpect(status().isOk())
				.andExpect(content().string("If an account exists for this email, a password reset link has been sent"));
	}

	@Test
	void resetPassword_Success_Returns200() throws Exception {
		ResetPasswordRequestDTO dto = new ResetPasswordRequestDTO();
		dto.setToken("reset-token");
		dto.setNewPassword("newPass123");

		doNothing().when(authService).resetPassword(any(ResetPasswordRequestDTO.class));

		mockMvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto))).andExpect(status().isOk())
				.andExpect(content().string("Password reset successfully"));
	}

	// Get profile

	@Test
	void getProfile_Success_ReturnsUser() throws Exception {
		when(authService.getUserProfile("test@gmail.com")).thenReturn(mockResponse);

		mockMvc.perform(get("/auth/profile")).andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("test@gmail.com"))
				.andExpect(jsonPath("$.name").value("Test User"));
	}

	// Update profile

	@Test
	void updateProfile_Success_Returns200() throws Exception {
		UpdateProfileDTO dto = new UpdateProfileDTO();
		dto.setName("Updated Name");

		when(authService.updateProfile(anyString(), any(UpdateProfileDTO.class))).thenReturn(mockResponse);

		mockMvc.perform(put("/auth/profile").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto))).andExpect(status().isOk());
	}

	// Change password

	@Test
	void changePassword_Success_Returns200() throws Exception {
		ChangePasswordDTO dto = new ChangePasswordDTO();
		dto.setOldPassword("oldPass123");
		dto.setNewPassword("newPass456");

		doNothing().when(authService).changePassword(anyString(), any());

		mockMvc.perform(put("/auth/password").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto))).andExpect(status().isOk())
				.andExpect(content().string("Password changed successfully"));
	}

	// Get all user 

	@Test
	void getAllUsers_ReturnsUserList() throws Exception {
		when(authService.getAllUsers()).thenReturn(List.of(mockResponse));

		// Set ADMIN role in security context
		UsernamePasswordAuthenticationToken adminAuth = new UsernamePasswordAuthenticationToken("admin@gmail.com", null,
				List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
		SecurityContextHolder.getContext().setAuthentication(adminAuth);

		mockMvc.perform(get("/auth/users")).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].email").value("test@gmail.com"));
	}

	// Deactivate user

	@Test
	void deactivateUser_Success_Returns200() throws Exception {
		doNothing().when(authService).deactivate(1L);

		mockMvc.perform(delete("/auth/user/1")).andExpect(status().isOk())
				.andExpect(content().string("Account deactivated successfully"));
	}

	@Test
	void deactivateUser_NotFound_Returns400() throws Exception {
		doThrow(new RuntimeException("User not found with id: 99")).when(authService).deactivate(99L);

		mockMvc.perform(delete("/auth/user/99")).andExpect(status().isBadRequest());
	}
}
