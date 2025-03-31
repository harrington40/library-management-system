package com.library.service;

import com.library.exception.*;
import com.library.dto.LoanDTO;
import com.library.dto.LoanRequestDTO;
import com.library.model.Book;
import com.library.model.Loan;
import com.library.model.Member;
import com.library.repository.BookRepository;
import com.library.repository.LoanRepository;
import com.library.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final ModelMapper modelMapper;

    @Value("${library.loan.duration.weeks:2}")
    private int loanDurationWeeks;

    @Value("${library.max-loans-per-member:5}")
    private int maxLoansPerMember;

    public LoanServiceImpl(LoanRepository loanRepository,
                         BookRepository bookRepository,
                         MemberRepository memberRepository,
                         ModelMapper modelMapper) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public LoanDTO loanBook(LoanRequestDTO loanRequest) {
        String bookId = loanRequest.getBookId();
        String memberId = loanRequest.getMemberId();

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));
        
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        validateBookAvailability(book);
        validateMemberLoanLimit(memberId);
        
        updateBookQuantity(book, -1);
        Loan loan = createLoanRecord(book, member);
        
        log.info("Book {} loaned to member {}", bookId, memberId);
        return convertToDTO(loan);
    }

    @Override
    public LoanDTO returnBook(String loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new LoanNotFoundException(loanId));

        if (loan.isReturned()) {
            throw new LoanAlreadyReturnedException(loanId);
        }

        // Load book if not already loaded
        if (loan.getBook() == null && loan.getBookId() != null) {
            loan.setBook(bookRepository.findById(loan.getBookId())
                .orElseThrow(() -> new BookNotFoundException(loan.getBookId())));
        }

        updateReturnDetails(loan);
        updateBookQuantity(loan.getBook(), 1);
        
        log.info("Book {} returned by member {}", 
                loan.getBook().getId(), loan.getMemberId());
        return convertToDTO(loan);
    }

    @Override
    public LoanDTO extendLoan(String loanId, int additionalWeeks) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new LoanNotFoundException(loanId));
        
        if (loan.isReturned()) {
            throw new LoanAlreadyReturnedException(loanId);
        }
        
        loan.setDueDate(loan.getDueDate().plusWeeks(additionalWeeks));
        loan = loanRepository.save(loan);
        
        log.info("Loan {} extended by {} weeks", loanId, additionalWeeks);
        return convertToDTO(loan);
    }

    @Override
    public Page<LoanDTO> getAllLoans(Pageable pageable) {
        return loanRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    @Override
    public List<LoanDTO> getLoansByMember(String memberId) {
        return loanRepository.findByMemberId(memberId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<LoanDTO> getActiveLoans(Pageable pageable) {
        return loanRepository.findByReturned(false, pageable)
                .map(this::convertToDTO);
    }

    @Override
    public Page<LoanDTO> getOverdueLoans(Pageable pageable) {
        LocalDate today = LocalDate.now();
        return loanRepository.findByReturnedAndDueDateBefore(false, today, pageable)
                .map(this::convertToDTO);
    }

    @Override
    public LoanDTO getLoanById(String loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new LoanNotFoundException(loanId));
        return convertToDTO(loan);
    }

    @Override
    public void deleteLoan(String loanId) {
        if (!loanRepository.existsById(loanId)) {
            throw new LoanNotFoundException(loanId);
        }
        loanRepository.deleteById(loanId);
    }

    // Helper methods
    private void validateBookAvailability(Book book) {
        if (book.getQuantity() <= 0 || !book.isAvailable()) {
            throw new BookNotAvailableException(book.getId());
        }
    }

    private void validateMemberLoanLimit(String memberId) {
        long activeLoans = loanRepository.countByMemberIdAndReturned(memberId, false);
        if (activeLoans >= maxLoansPerMember) {
            throw new MaxLoansExceededException(memberId, maxLoansPerMember);
        }
    }

    private void updateBookQuantity(Book book, int delta) {
        int newQuantity = book.getQuantity() + delta;
        book.setQuantity(newQuantity);
        book.setAvailable(newQuantity > 0);
        bookRepository.save(book);
    }

    private Loan createLoanRecord(Book book, Member member) {
        Loan loan = new Loan();
        loan.setBook(book);
        loan.setMember(member);
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusWeeks(loanDurationWeeks));
        loan.setReturned(false);
        return loanRepository.save(loan);
    }

    private void updateReturnDetails(Loan loan) {
        loan.setReturnDate(LocalDate.now());
        loan.setReturned(true);
        loan.setLate(loan.getReturnDate().isAfter(loan.getDueDate()));
        loanRepository.save(loan);
    }

    private LoanDTO convertToDTO(Loan loan) {
        LoanDTO dto = modelMapper.map(loan, LoanDTO.class);
        
        // Handle book reference
        if (loan.getBook() != null) {
            dto.setBookId(loan.getBook().getId());
            dto.setBookTitle(loan.getBook().getTitle());
        } else {
            dto.setBookId(loan.getBookId());
            // Optionally load title if needed for DTO
            if (loan.getBookId() != null) {
                dto.setBookTitle(bookRepository.findById(loan.getBookId())
                    .map(Book::getTitle)
                    .orElse("Unknown Book"));
            }
        }
        
        // Handle member reference
        if (loan.getMember() != null) {
            dto.setMemberId(loan.getMember().getId());
            dto.setMemberName(loan.getMember().getFullName());
        } else {
            dto.setMemberId(loan.getMemberId());
            // Optionally load member name if needed for DTO
            if (loan.getMemberId() != null) {
                dto.setMemberName(memberRepository.findById(loan.getMemberId())
                    .map(Member::getFullName)
                    .orElse("Unknown Member"));
            }
        }
        
        return dto;
    }
}