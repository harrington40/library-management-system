package com.library.exception;

public class LoanAlreadyReturnedException extends RuntimeException {
    public LoanAlreadyReturnedException(String id) {
        super("Loan already returned with ID: " + id);
    }
}