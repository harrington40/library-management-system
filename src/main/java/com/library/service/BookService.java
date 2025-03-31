package com.library.service;

import com.library.model.Book;
import com.library.repository.BookRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class BookService {
    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public Optional<Book> getBookById(String id) {
        return bookRepository.findById(id);
    }

    public Book addBook(Book book) {
        book.setAvailable(book.getQuantity() > 0);
        return bookRepository.save(book);
    }

    public Book updateBook(String id, Book bookDetails) {
        return bookRepository.findById(id)
                .map(book -> {
                    book.setTitle(bookDetails.getTitle());
                    book.setAuthor(bookDetails.getAuthor());
                    book.setIsbn(bookDetails.getIsbn());
                    book.setPublicationYear(bookDetails.getPublicationYear());
                    book.setQuantity(bookDetails.getQuantity());
                    book.setAvailable(bookDetails.getQuantity() > 0);
                    return bookRepository.save(book);
                })
                .orElseGet(() -> {
                    bookDetails.setId(id);
                    return bookRepository.save(bookDetails);
                });
    }

    public void deleteBook(String id) {
        bookRepository.deleteById(id);
    }

    public List<Book> searchBooksByTitle(String title) {
        return bookRepository.findByTitleContainingIgnoreCase(title);
    }

    public List<Book> searchBooksByAuthor(String author) {
        return bookRepository.findByAuthorContainingIgnoreCase(author);
    }

    public List<Book> getAvailableBooks() {
        return bookRepository.findByAvailable(true);
    }
}