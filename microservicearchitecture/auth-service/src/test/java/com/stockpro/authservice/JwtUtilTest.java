package com.stockpro.authservice;

import com.stockpro.authservice.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

	private JwtUtil jwtUtil;

	@BeforeEach
	void setUp() {
		jwtUtil = new JwtUtil();
		ReflectionTestUtils.setField(jwtUtil, "secret",
				"5A7234753778214125442A472D4B6150645367566B59703373367639792F423F");
		ReflectionTestUtils.setField(jwtUtil, "expiration", 28800000L);
		ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", 604800000L);
	}

	@Test
	void generateToken_ReturnsNonNull() {
		String token = jwtUtil.generateToken("test@gmail.com", "STAFF");
		assertNotNull(token);
		assertFalse(token.isEmpty());
	}

	@Test
	void generateToken_ExtractUsernameCorrect() {
		String token = jwtUtil.generateToken("test@gmail.com", "STAFF");
		assertEquals("test@gmail.com", jwtUtil.extractUsername(token));
	}

	@Test
	void generateToken_ExtractRoleCorrect() {
		String token = jwtUtil.generateToken("test@gmail.com", "ADMIN");
		assertEquals("ADMIN", jwtUtil.extractRole(token));
	}

	@Test
	void generateRefreshToken_TypeIsRefresh() {
		String token = jwtUtil.generateRefreshToken("test@gmail.com");
		assertEquals("REFRESH", jwtUtil.extractType(token));
	}

	@Test
	void generateToken_TypeIsAccess() {
		String token = jwtUtil.generateToken("test@gmail.com", "STAFF");
		assertEquals("ACCESS", jwtUtil.extractType(token));
	}

	@Test
	void validateToken_ValidToken_ReturnsTrue() {
		String token = jwtUtil.generateToken("test@gmail.com", "STAFF");
		assertTrue(jwtUtil.validateToken(token));
	}

	@Test
	void validateToken_InvalidToken_ReturnsFalse() {
		assertFalse(jwtUtil.validateToken("invalid.token.here"));
	}

	@Test
	void validateToken_BlacklistedToken_ReturnsFalse() {
		String token = jwtUtil.generateToken("test@gmail.com", "STAFF");
		jwtUtil.blacklistToken(token);
		assertFalse(jwtUtil.validateToken(token));
	}

	@Test
	void blacklistToken_TokenIsBlacklisted() {
		String token = jwtUtil.generateToken("test@gmail.com", "STAFF");
		assertFalse(jwtUtil.isBlacklisted(token));
		jwtUtil.blacklistToken(token);
		assertTrue(jwtUtil.isBlacklisted(token));
	}

	@Test
	void validateToken_NullToken_ReturnsFalse() {
		assertFalse(jwtUtil.validateToken(null));
	}

	@Test
	void validateToken_EmptyToken_ReturnsFalse() {
		assertFalse(jwtUtil.validateToken(""));
	}

	@Test
	void generateRefreshToken_UsernameCorrect() {
		String token = jwtUtil.generateRefreshToken("user@gmail.com");
		assertEquals("user@gmail.com", jwtUtil.extractUsername(token));
	}
}