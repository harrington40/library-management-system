package com.library.exception;

public class BookNotAvailableException extends RuntimeException {
    public BookNotAvailableException(String id) {
        super("Book not available with ID: " + id);
    }
}