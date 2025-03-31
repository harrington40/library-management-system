package com.library.controller;

import com.library.dto.LoanDTO;
import com.library.dto.LoanRequestDTO;
import com.library.service.LoanService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    public ResponseEntity<LoanDTO> loanBook(@RequestBody LoanRequestDTO loanRequest) {
        return ResponseEntity.ok(loanService.loanBook(loanRequest));
    }

    @PutMapping("/{loanId}/return")
    public ResponseEntity<LoanDTO> returnBook(@PathVariable String loanId) {
        return ResponseEntity.ok(loanService.returnBook(loanId));
    }

    @PutMapping("/{loanId}/extend")
    public ResponseEntity<LoanDTO> extendLoan(
            @PathVariable String loanId,
            @RequestParam int additionalWeeks) {
        return ResponseEntity.ok(loanService.extendLoan(loanId, additionalWeeks));
    }

    @GetMapping
    public ResponseEntity<Page<LoanDTO>> getAllLoans(Pageable pageable) {
        return ResponseEntity.ok(loanService.getAllLoans(pageable));
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<LoanDTO>> getLoansByMember(@PathVariable String memberId) {
        return ResponseEntity.ok(loanService.getLoansByMember(memberId));
    }

    @GetMapping("/active")
    public ResponseEntity<Page<LoanDTO>> getActiveLoans(Pageable pageable) {
        return ResponseEntity.ok(loanService.getActiveLoans(pageable));
    }

    @GetMapping("/overdue")
    public ResponseEntity<Page<LoanDTO>> getOverdueLoans(Pageable pageable) {
        return ResponseEntity.ok(loanService.getOverdueLoans(pageable));
    }

    @GetMapping("/{loanId}")
    public ResponseEntity<LoanDTO> getLoanById(@PathVariable String loanId) {
        return ResponseEntity.ok(loanService.getLoanById(loanId));
    }

    @DeleteMapping("/{loanId}")
    public ResponseEntity<Void> deleteLoan(@PathVariable String loanId) {
        loanService.deleteLoan(loanId);
        return ResponseEntity.noContent().build();
    }
}