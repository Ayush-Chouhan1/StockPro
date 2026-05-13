package com.stockpro.authservice.exception;

public class UserAlreadyExistsException extends RuntimeException {

	
	public UserAlreadyExistsException(String message) {
		super(message);
	}
}
