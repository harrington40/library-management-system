package com.library.service;

import com.library.dto.LoanDTO;
import com.library.dto.LoanRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LoanService {
    LoanDTO loanBook(LoanRequestDTO loanRequest);
    LoanDTO returnBook(String loanId);
    LoanDTO extendLoan(String loanId, int additionalWeeks);
    Page<LoanDTO> getAllLoans(Pageable pageable);
    List<LoanDTO> getLoansByMember(String memberId);
    Page<LoanDTO> getActiveLoans(Pageable pageable);
    Page<LoanDTO> getOverdueLoans(Pageable pageable);
    LoanDTO getLoanById(String loanId);
    void deleteLoan(String loanId);
}