package com.stockpro.product_service.exception;

public class DuplicateSkuException extends RuntimeException {
    public DuplicateSkuException(String message) {
        super(message);
    }
}