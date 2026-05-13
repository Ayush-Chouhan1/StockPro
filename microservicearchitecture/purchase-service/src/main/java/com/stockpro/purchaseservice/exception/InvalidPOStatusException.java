package com.stockpro.purchaseservice.exception;

public class InvalidPOStatusException extends RuntimeException {
    public InvalidPOStatusException(String message) {
        super(message);
    }
}