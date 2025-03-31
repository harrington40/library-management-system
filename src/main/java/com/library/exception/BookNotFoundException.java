package com.library.exception;

public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException(String id) {
        super("Book not found with ID: " + id);
    }
}