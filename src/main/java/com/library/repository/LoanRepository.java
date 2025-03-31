package com.library.repository;

import com.library.model.Loan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.LocalDate;
import java.util.List;

public interface LoanRepository extends MongoRepository<Loan, String> {
    List<Loan> findByMemberId(String memberId);
    long countByMemberIdAndReturned(String memberId, boolean returned);
    Page<Loan> findByReturned(boolean returned, Pageable pageable);
    Page<Loan> findByReturnedAndDueDateBefore(boolean returned, LocalDate dueDate, Pageable pageable);
}