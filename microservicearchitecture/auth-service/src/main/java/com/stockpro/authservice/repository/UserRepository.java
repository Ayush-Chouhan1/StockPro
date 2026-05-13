package com.stockpro.authservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stockpro.authservice.entity.User;
 

public interface UserRepository extends JpaRepository<User, Long> {
	
	Optional<User> findByEmail(String email);

	Optional<User> findByResetPasswordToken(String resetPasswordToken);

	boolean existsByEmail(String email);
}
