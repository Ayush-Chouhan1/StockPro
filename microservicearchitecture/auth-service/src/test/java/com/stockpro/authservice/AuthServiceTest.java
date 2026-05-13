package com.stockpro.authservice;

import com.stockpro.authservice.dto.*;
import com.stockpro.authservice.entity.User;
import com.stockpro.authservice.entity.UserRole;
import com.stockpro.authservice.exception.UserAlreadyExistsException;
import com.stockpro.authservice.repository.UserRepository;
import com.stockpro.authservice.security.JwtUtil;
import com.stockpro.authservice.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtUtil jwtUtil;

	@Mock
	private JavaMailSender mailSender;

	@InjectMocks
	private AuthService authService;

	private User mockUser;
	private UserRequestDTO requestDTO;

	@BeforeEach
	void setUp() {
		mockUser = new User();
		mockUser.setId(1L);
		mockUser.setName("Test User");
		mockUser.setEmail("test@gmail.com");
		mockUser.setPassword("encodedPassword");
		mockUser.setPhone("9999999999");
		mockUser.setRole(UserRole.WAREHOUSE_STAFF);
		mockUser.setDepartment("IT");
		mockUser.setIsActive(true);
		mockUser.setCreatedAt(LocalDateTime.now());

		requestDTO = new UserRequestDTO();
		requestDTO.setName("Test User");
		requestDTO.setEmail("test@gmail.com");
		requestDTO.setPassword("password123");
		requestDTO.setPhone("9999999999");
		requestDTO.setRole(UserRole.WAREHOUSE_STAFF);
		requestDTO.setDepartment("IT");

		ReflectionTestUtils.setField(authService, "resetPasswordFrontendUrl", "http://localhost:3000/reset-password");
		ReflectionTestUtils.setField(authService, "resetPasswordTokenExpirationMinutes", 15L);
		ReflectionTestUtils.setField(authService, "mailFrom", "no-reply@stockpro.local");
	}

	// Register test
	@Test
	void register_Success() {
		when(userRepository.existsByEmail(anyString())).thenReturn(false);
		when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
		when(userRepository.save(any(User.class))).thenReturn(mockUser);

		RegisterResponseDTO result = authService.register(requestDTO);

		assertEquals(1L, result.getUserId());
		assertEquals("User registered successfully", result.getMessage());
		verify(userRepository).existsByEmail("test@gmail.com");
		verify(userRepository).save(any(User.class));
	}

	@Test
	void register_EmailAlreadyExists_ThrowsException() {
		when(userRepository.existsByEmail(anyString())).thenReturn(true);

		assertThrows(UserAlreadyExistsException.class, () -> authService.register(requestDTO));

		verify(userRepository, never()).save(any());
	}

	@Test
	void register_AdminRole_ThrowsException() {
		requestDTO.setRole(UserRole.ADMIN);
		when(userRepository.existsByEmail(anyString())).thenReturn(false);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> authService.register(requestDTO));

		assertEquals("Admin accounts must be created by an Admin user", ex.getMessage());
		verify(userRepository, never()).save(any());
	}

	@Test
	void register_EncodesPassword() {
		when(userRepository.existsByEmail(anyString())).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
		when(userRepository.save(any(User.class))).thenReturn(mockUser);

		authService.register(requestDTO);

		verify(passwordEncoder).encode("password123");
	}

	// Login test
	@Test
	void login_Success() {
		when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
		when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
		when(jwtUtil.generateToken("test@gmail.com", "WAREHOUSE_STAFF")).thenReturn("access-token");
		when(jwtUtil.generateRefreshToken("test@gmail.com")).thenReturn("refresh-token");
		when(userRepository.save(any(User.class))).thenReturn(mockUser);

		LoginResponseDTO result = authService.login("test@gmail.com", "password123");

		assertNotNull(result);
		assertEquals("access-token", result.getAccessToken());
		assertEquals("refresh-token", result.getRefreshToken());
	}

	@Test
	void login_UserNotFound_ThrowsException() {
		when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

		assertThrows(RuntimeException.class, () -> authService.login("wrong@gmail.com", "password123"));
	}

	@Test
	void login_WrongPassword_ThrowsException() {
		when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
		when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

		assertThrows(RuntimeException.class, () -> authService.login("test@gmail.com", "wrongpassword"));
	}

	@Test
	void login_DeactivatedAccount_ThrowsException() {
		mockUser.setIsActive(false);
		when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));

		RuntimeException ex = assertThrows(RuntimeException.class,
				() -> authService.login("test@gmail.com", "password123"));

		assertEquals("Account is deactivated", ex.getMessage());
	}

	@Test
	void login_UpdatesLastLoginAt() {
		when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
		when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
		when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("token");
		when(jwtUtil.generateRefreshToken(anyString())).thenReturn("refresh");
		when(userRepository.save(any(User.class))).thenReturn(mockUser);

		authService.login("test@gmail.com", "password123");

		verify(userRepository).save(argThat(u -> u.getLastLoginAt() != null));
	}

      // Logout
	
	
	@Test
	void logout_BlacklistsToken() {
		authService.logout("some-token");
		verify(jwtUtil).blacklistToken("some-token");
	}

	//Refresh test

	@Test
	void refresh_Success() {
		when(jwtUtil.validateToken("refresh-token")).thenReturn(true);
		when(jwtUtil.extractType("refresh-token")).thenReturn("REFRESH");
		when(jwtUtil.extractUsername("refresh-token")).thenReturn("test@gmail.com");
		when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
		when(jwtUtil.generateToken("test@gmail.com", "WAREHOUSE_STAFF")).thenReturn("new-access-token");
		when(jwtUtil.generateRefreshToken("test@gmail.com")).thenReturn("new-refresh-token");

		LoginResponseDTO result = authService.refresh("refresh-token");

		assertNotNull(result);
		assertEquals("new-access-token", result.getAccessToken());
		verify(jwtUtil).blacklistToken("refresh-token");
	}

	@Test
	void refresh_InvalidToken_ThrowsException() {
		when(jwtUtil.validateToken("bad-token")).thenReturn(false);

		assertThrows(RuntimeException.class, () -> authService.refresh("bad-token"));
	}

	@Test
	void refresh_NotRefreshType_ThrowsException() {
		when(jwtUtil.validateToken("access-token")).thenReturn(true);
		when(jwtUtil.extractType("access-token")).thenReturn("ACCESS");

		assertThrows(RuntimeException.class, () -> authService.refresh("access-token"));
	}

	// get profile tests

	@Test
	void getUserProfile_Success() {
		when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));

		UserResponseDTO result = authService.getUserProfile("test@gmail.com");

		assertNotNull(result);
		assertEquals("test@gmail.com", result.getEmail());
		assertEquals("Test User", result.getName());
		assertEquals(UserRole.WAREHOUSE_STAFF, result.getRole());
	}

	@Test
	void getUserProfile_NotFound_ThrowsException() {
		when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

		assertThrows(RuntimeException.class, () -> authService.getUserProfile("notfound@gmail.com"));
	}

	// Update profile test

	@Test
	void updateProfile_Success() {
		UpdateProfileDTO dto = new UpdateProfileDTO();
		dto.setName("Updated Name");
		dto.setPhone("8888888888");
		dto.setDepartment("HR");

		when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
		when(userRepository.save(any(User.class))).thenReturn(mockUser);

		UserResponseDTO result = authService.updateProfile("test@gmail.com", dto);

		assertNotNull(result);
		verify(userRepository).save(any(User.class));
	}

	@Test
	void updateProfile_UserNotFound_ThrowsException() {
		UpdateProfileDTO dto = new UpdateProfileDTO();
		dto.setName("Name");

		when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

		assertThrows(RuntimeException.class, () -> authService.updateProfile("notfound@gmail.com", dto));
	}

	// Change password test

	@Test
	void changePassword_Success() {
		ChangePasswordDTO dto = new ChangePasswordDTO();
		dto.setOldPassword("oldPass123");
		dto.setNewPassword("newPass123");

		when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
		when(passwordEncoder.matches("oldPass123", "encodedPassword")).thenReturn(true);
		when(passwordEncoder.encode("newPass123")).thenReturn("newEncodedPass");
		when(userRepository.save(any(User.class))).thenReturn(mockUser);

		assertDoesNotThrow(() -> authService.changePassword("test@gmail.com", dto));
		verify(userRepository).save(any(User.class));
	}

	@Test
	void changePassword_WrongOldPassword_ThrowsException() {
		ChangePasswordDTO dto = new ChangePasswordDTO();
		dto.setOldPassword("wrongOld");
		dto.setNewPassword("newPass123");

		when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
		when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

		RuntimeException ex = assertThrows(RuntimeException.class,
				() -> authService.changePassword("test@gmail.com", dto));

		assertEquals("Old password is incorrect", ex.getMessage());
	}

	@Test
	void requestPasswordReset_ExistingActiveUser_SavesTokenAndSendsEmail() {
		ForgotPasswordRequestDTO dto = new ForgotPasswordRequestDTO();
		dto.setEmail("test@gmail.com");

		when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(mockUser));
		when(userRepository.save(any(User.class))).thenReturn(mockUser);

		authService.requestPasswordReset(dto);

		verify(userRepository).save(argThat(user ->
				user.getResetPasswordToken() != null
						&& user.getResetPasswordTokenExpiresAt() != null));
		verify(mailSender).send(any(SimpleMailMessage.class));
	}

	@Test
	void requestPasswordReset_UnknownEmail_DoesNotRevealAccountExistence() {
		ForgotPasswordRequestDTO dto = new ForgotPasswordRequestDTO();
		dto.setEmail("missing@gmail.com");

		when(userRepository.findByEmail("missing@gmail.com")).thenReturn(Optional.empty());

		assertDoesNotThrow(() -> authService.requestPasswordReset(dto));
		verify(userRepository, never()).save(any());
		verify(mailSender, never()).send(any(SimpleMailMessage.class));
	}

	@Test
	void resetPassword_ValidToken_UpdatesPasswordAndClearsToken() {
		ResetPasswordRequestDTO dto = new ResetPasswordRequestDTO();
		dto.setToken("reset-token");
		dto.setNewPassword("newPass123");
		mockUser.setResetPasswordToken("reset-token");
		mockUser.setResetPasswordTokenExpiresAt(LocalDateTime.now().plusMinutes(10));

		when(userRepository.findByResetPasswordToken("reset-token")).thenReturn(Optional.of(mockUser));
		when(passwordEncoder.encode("newPass123")).thenReturn("newEncodedPass");
		when(userRepository.save(any(User.class))).thenReturn(mockUser);

		authService.resetPassword(dto);

		verify(userRepository).save(argThat(user ->
				"newEncodedPass".equals(user.getPassword())
						&& user.getResetPasswordToken() == null
						&& user.getResetPasswordTokenExpiresAt() == null));
	}

	@Test
	void resetPassword_ExpiredToken_ThrowsExceptionAndClearsToken() {
		ResetPasswordRequestDTO dto = new ResetPasswordRequestDTO();
		dto.setToken("expired-token");
		dto.setNewPassword("newPass123");
		mockUser.setResetPasswordToken("expired-token");
		mockUser.setResetPasswordTokenExpiresAt(LocalDateTime.now().minusMinutes(1));

		when(userRepository.findByResetPasswordToken("expired-token")).thenReturn(Optional.of(mockUser));
		when(userRepository.save(any(User.class))).thenReturn(mockUser);

		RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.resetPassword(dto));

		assertEquals("Invalid or expired reset token", ex.getMessage());
		verify(userRepository).save(argThat(user ->
				user.getResetPasswordToken() == null
						&& user.getResetPasswordTokenExpiresAt() == null));
		verify(passwordEncoder, never()).encode(anyString());
	}

	// Get all user test

	@Test
	void getAllUsers_ReturnsList() {
		when(userRepository.findAll()).thenReturn(List.of(mockUser));

		List<UserResponseDTO> result = authService.getAllUsers();

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals("test@gmail.com", result.get(0).getEmail());
	}

	@Test
	void getAllUsers_EmptyList() {
		when(userRepository.findAll()).thenReturn(List.of());

		List<UserResponseDTO> result = authService.getAllUsers();

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	//

	@Test
	void deactivate_Success() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
		when(userRepository.save(any(User.class))).thenReturn(mockUser);

		assertDoesNotThrow(() -> authService.deactivate(1L));
		verify(userRepository).save(argThat(u -> !u.getIsActive()));
	}

	@Test
	void deactivate_UserNotFound_ThrowsException() {
		when(userRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(RuntimeException.class, () -> authService.deactivate(99L));
	}
}
