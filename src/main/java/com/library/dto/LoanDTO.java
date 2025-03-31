package com.library.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class LoanDTO {
    private String id;
    private String bookId;
    private String bookTitle;
    private String memberId;
    private String memberName;
    private LocalDate loanDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private boolean returned;
    private boolean late;
}