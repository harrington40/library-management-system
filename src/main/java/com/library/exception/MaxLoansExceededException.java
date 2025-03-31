package com.library.exception;

public class MaxLoansExceededException extends RuntimeException {
    public MaxLoansExceededException(String memberId, int maxLoans) {
        super(String.format("Member %s has reached the maximum allowed loans (%d)", 
              memberId, maxLoans));
    }
}