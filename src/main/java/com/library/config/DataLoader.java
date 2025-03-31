package com.library.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.model.*;
import com.library.repository.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);

    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${library.default-users.admin.password:admin123}")
    private String adminPassword;

    @Value("${library.default-users.librarian.password:lib123}")
    private String librarianPassword;

    @Value("${library.default-users.assistant.password:ast123}")
    private String assistantPassword;

    public DataLoader(BookRepository bookRepository,
                    MemberRepository memberRepository,
                    UserRepository userRepository,
                    ObjectMapper objectMapper,
                    PasswordEncoder passwordEncoder) {
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        try {
            initializeDefaultUsers();
            loadSampleDataIfEmpty();
        } catch (Exception e) {
            logger.error("Data initialization failed", e);
            throw new DataInitializationException("Failed to initialize application data", e);
        }
    }

    private void initializeDefaultUsers() {
        updateOrCreateUser("admin", adminPassword, "ADMIN");
        updateOrCreateUser("librarian", librarianPassword, "LIBRARIAN");
        updateOrCreateUser("assistant", assistantPassword, "ASSISTANT");
    }

    private void updateOrCreateUser(String username, String plainPassword, String role) {
        userRepository.findByUsername(username).ifPresentOrElse(
            existingUser -> updatePasswordIfChanged(existingUser, plainPassword),
            () -> createNewUser(username, plainPassword, role)
        );
    }

    private void updatePasswordIfChanged(User user, String plainPassword) {
        if (!passwordEncoder.matches(plainPassword, user.getPassword())) {
            user.setPassword(passwordEncoder.encode(plainPassword));
            userRepository.save(user);
            logger.info("Updated password for {} user", user.getUsername());
        } else {
            logger.debug("Password for {} user unchanged", user.getUsername());
        }
    }

    private void createNewUser(String username, String plainPassword, String role) {
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(plainPassword));
        newUser.setRoles(List.of("ROLE_LIBRARIAN"));  // Correct method name
        userRepository.save(newUser);
        logger.info("Created new {} user with role {}", username, role);
    }

    private void loadSampleDataIfEmpty() throws IOException {
        if (bookRepository.count() == 0 && memberRepository.count() == 0) {
            try (InputStream inputStream = new ClassPathResource("data.json").getInputStream()) {
                SampleData sampleData = objectMapper.readValue(inputStream, SampleData.class);
                validateSampleData(sampleData);
                
                bookRepository.saveAll(sampleData.getBooks());
                memberRepository.saveAll(sampleData.getMembers());
                
                logger.info("Loaded {} books and {} members", 
                          sampleData.getBooks().size(), 
                          sampleData.getMembers().size());
            }
        }
    }

    private void validateSampleData(SampleData sampleData) {
        if (sampleData.getBooks() == null || sampleData.getMembers() == null) {
            throw new IllegalStateException("Invalid data.json structure");
        }
    }

    private static class SampleData {
        private List<Book> books;
        private List<Member> members;

        public List<Book> getBooks() { return books; }
        public void setBooks(List<Book> books) { this.books = books; }
        public List<Member> getMembers() { return members; }
        public void setMembers(List<Member> members) { this.members = members; }
    }

    private static class DataInitializationException extends RuntimeException {
        public DataInitializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @PostConstruct
public void validateConnection() {
    try {
        mongoTemplate.executeCommand("{ ping: 1 }");
    } catch (Exception e) {
        logger.error("MongoDB connection failed", e);
        throw new IllegalStateException("Cannot connect to MongoDB", e);
    }
}
}