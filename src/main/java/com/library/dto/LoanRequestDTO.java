package com.library.dto;

import lombok.Data;

@Data
public class LoanRequestDTO {
    private String bookId;
    private String memberId;
}