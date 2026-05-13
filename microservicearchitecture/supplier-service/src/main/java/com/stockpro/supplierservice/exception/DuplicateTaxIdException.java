package com.stockpro.supplierservice.exception;

public class DuplicateTaxIdException extends RuntimeException {
    public DuplicateTaxIdException(String message) {
        super(message);
    }
}