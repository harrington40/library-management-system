package com.library.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;

@Data
@Document(collection = "loans")
public class Loan {
    @Id
    private String id;
    
    // Reference IDs (stored in DB)
    private String bookId;
    private String memberId;
    
    // Transient fields (not stored in DB)
    @Transient
    private Book book;
    
    @Transient
    private Member member;
    
    private LocalDate loanDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private boolean returned;
    private boolean late;

    // Manual getters/setters for transient fields
    public Book getBook() {
        return this.book;
    }
    
    public void setBook(Book book) {
        this.book = book;
        if (book != null) {
            this.bookId = book.getId();
        }
    }
    
    public Member getMember() {
        return this.member;
    }
    
    public void setMember(Member member) {
        this.member = member;
        if (member != null) {
            this.memberId = member.getId();
        }
    }
}