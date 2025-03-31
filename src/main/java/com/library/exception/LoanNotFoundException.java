package com.library.exception;

public class LoanNotFoundException extends RuntimeException {
    public LoanNotFoundException(String id) {
        super("Loan not found with ID: " + id);
    }
}