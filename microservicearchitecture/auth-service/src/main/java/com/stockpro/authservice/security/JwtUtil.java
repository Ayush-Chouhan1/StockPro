package com.stockpro.authservice.security;

import java.security.Key;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

	@Value("${jwt.secret}")
	private String secret;

	@Value("${jwt.expiration}")
	private long expiration;

	@Value("${jwt.refresh-expiration}")
	private long refreshExpiration;

	private final Set<String> blacklistedTokens = new HashSet<>();

	private Key getKey() {
		return Keys.hmacShaKeyFor(secret.getBytes());
	}

	public String generateToken(String email, String role) {
		return Jwts.builder().setSubject(email)
				.claim("role", role).claim("type", "ACCESS")
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + expiration))
				.signWith(getKey(), SignatureAlgorithm.HS256)
				.compact();
	}

	public String generateRefreshToken(String email) {
		return Jwts.builder().setSubject(email)
				.claim("type", "REFRESH")
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + refreshExpiration))
				.signWith(getKey(), SignatureAlgorithm.HS256)
				.compact();
	}

	public String extractUsername(String token) {
		return getClaims(token).getSubject();
	}

	public String extractRole(String token) {
		return getClaims(token).get("role", String.class);
	}

	public String extractType(String token) {
		return getClaims(token).get("type", String.class);
	}

	public boolean validateToken(String token) {
		try {
			if (blacklistedTokens.contains(token))
				return false;
			getClaims(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public void blacklistToken(String token) {
		blacklistedTokens.add(token);
	}

	public boolean isBlacklisted(String token) {
		return blacklistedTokens.contains(token);
	}

	private Claims getClaims(String token) {
		return Jwts.parserBuilder().setSigningKey(getKey()).build().parseClaimsJws(token).getBody();
	}
}