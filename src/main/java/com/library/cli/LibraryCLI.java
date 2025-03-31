package com.library.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Scanner;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

public class LibraryCLI {
    private static  String BASE_URL = "http://localhost:8080/api";
    private static  HttpClient httpClient = HttpClient.newHttpClient();
    private static  Scanner scanner = new Scanner(System.in);
    private static  ObjectMapper objectMapper = new ObjectMapper();
    
    private static String jwtToken;  // Stores the JWT token after login
    private static Role currentUserRole;  
    

 private static String generateBookId() {
    Random random = new Random();
    int part1 = random.nextInt(100); // 00-99
    int part2 = random.nextInt(10000); // 0000-9999
    return String.format("%02d-%04d", part1, part2);

}


    public enum Role {
        ROLE_ADMIN(EnumSet.allOf(Permission.class)),
        ROLE_LIBRARIAN(EnumSet.of(
            Permission.MANAGE_BOOKS,
            Permission.MANAGE_LOANS,
            Permission.VIEW_MEMBERS,
            Permission.VIEW_BOOKS
        )),
        ROLE_ASSISTANT(EnumSet.of(
            Permission.MANAGE_LOANS,
            Permission.VIEW_BOOKS
            
        ));
        
        private final Set<Permission> permissions;
        
        Role(Set<Permission> permissions) {
            this.permissions = permissions;
        }
        
        public boolean hasPermission(Permission permission) {
            return permissions.contains(permission);
        }
    }

      // Add this method

    
    public enum Permission {
        MANAGE_BOOKS,
        MANAGE_MEMBERS,
        MANAGE_LOANS,
        MANAGE_USERS,
        VIEW_BOOKS,
        VIEW_MEMBERS
    }

    public static void main(String[] args) {
        System.out.println(" Build successful!");
        System.out.println(" Running the application...\n");
        
        if (!login()) {
            System.out.println("Login failed. Exiting application.");
            return;
        }

        while (true) {
            displayMainMenu();
            int choice = getMenuChoice();
            handleMenuChoice(choice);
        }
    }

    private static void displayMainMenu() {
        System.out.println("\n=== Library Management System ===");
        System.out.println("Logged in as: " + currentUserRole.name());
        System.out.println("1. Book Operations");
        System.out.println("2. Member Operations");
        System.out.println("3. Loan Operations");
        
        if (currentUserRole.hasPermission(Permission.MANAGE_USERS)) {
            System.out.println("4. User Management");
            System.out.println("5. Exit");
        } else {
            System.out.println("4. Exit");
        }
    }

    private static int getMenuChoice() {
        System.out.print("Select option: ");
        int choice = scanner.nextInt();
        scanner.nextLine();
        return choice;
    }

    private static void handleMenuChoice(int choice) {
        switch (choice) {
            case 1 -> bookOperations();
            case 2 -> memberOperations();
            case 3 -> loanOperations();
            case 4 -> {
                if (currentUserRole.hasPermission(Permission.MANAGE_USERS)) {
                    userManagement();
                } else {
                    exitApplication();
                }
            }
            case 5 -> {
                if (currentUserRole.hasPermission(Permission.MANAGE_USERS)) {
                    exitApplication();
                } else {
                    System.out.println("Invalid option!");
                }
            }
            default -> System.out.println("Invalid option!");
        }
    }

    private static boolean login() {
        System.out.println("\n=== Library System Login ===");
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        
        String json = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", 
                      username, password);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        
        try {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            JsonNode responseNode = objectMapper.readTree(response.body());
            jwtToken = responseNode.path("token").asText();
            
            // Handle role conversion
            String roleStr = responseNode.path("role").asText();
            currentUserRole = Role.valueOf(roleStr); // Now matches ROLE_ADMIN format
            System.out.println("Role from server: " + roleStr); 
            System.out.println("Login successful! Welcome, " + username);
            return true;
        } else {
            System.err.println("Error: " + response.body());
        }
    } catch (Exception e) {
        System.err.println("Login error: " + e.getMessage());
        System.err.println("Valid roles are: " + Arrays.toString(Role.values()));
    }
        System.out.println("Invalid credentials!");
        return false;
    }

    private static void bookOperations() {
        System.out.println("\n=== Book Operations ===");
        System.out.println("1. List all books");
        System.out.println("2. Add new book");
        System.out.println("3. Update book");
        System.out.println("4. Back to main menu");
        System.out.print("Select option: ");
        
        int choice = scanner.nextInt();
        scanner.nextLine();
        
        switch (choice) {
            case 1 -> listAllBooks();
            case 2 -> addNewBook();
            case 3 -> updateBook();
            case 4 -> { return; }
            default -> System.out.println("Invalid option!");
        }
    }

    private static void listAllBooks() {
        if (!currentUserRole.hasPermission(Permission.VIEW_BOOKS)) {
            System.out.println("Unauthorized access! Your role (" + currentUserRole + 
            ") doesn't have permission to view books.");
            System.out.println("Required permission: VIEW_BOOKS");
            return;
        }
        
        HttpRequest request = createAuthenticatedRequestBuilder("/books")
                .GET()
                .build();
        
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            handleAuthResponse(response);
            
            JsonNode booksNode = objectMapper.readTree(response.body());
            if (booksNode.isArray() && booksNode.size() > 0) {
                printBooksTable(booksNode);
            } else {
                System.out.println("\nNo books found in the catalog.");
            }
        } catch (Exception e) {
            System.err.println("Error fetching books: " + e.getMessage());
        }
    }

    private static void printBooksTable(JsonNode booksNode) {
        System.out.println("\n+---------------------------- BOOK CATALOG ----------------------------+");
        System.out.println("| ID        | Title                | Author             | Year | Qty | Available |");
        System.out.println("+-----------+----------------------+--------------------+------+-----+-----------+");
        
        for (JsonNode bookNode : booksNode) {
            System.out.printf("| %-9s | %-20s | %-18s | %-4d | %-3d | %-9s |%n",
                    shortenId(bookNode.path("id").asText()),
                    truncate(bookNode.path("title").asText(), 20),
                    truncate(bookNode.path("author").asText(), 18),
                    bookNode.path("publicationYear").asInt(),
                    bookNode.path("quantity").asInt(),
                    bookNode.path("available").asBoolean() ? "Yes" : "No");
        }
        System.out.println("+-----------+----------------------+--------------------+------+-----+-----------+");
    }

    private static void addNewBook() {
        if (!currentUserRole.hasPermission(Permission.MANAGE_BOOKS)) {
            System.out.println("Unauthorized access!");
            return;
        }
        
        System.out.println("\nAdd New Book");
        BookDetails details = getBookDetailsFromUser();
        
        HttpRequest request = createAuthenticatedRequestBuilder("/books")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(details.toJson()))
                .build();

        sendRequestAndHandleResponse(request, "Book added successfully");
    }

    private static BookDetails getBookDetailsFromUser() {
        System.out.print("Title: ");
        String title = scanner.nextLine();
        System.out.print("Author: ");
        String author = scanner.nextLine();
        System.out.print("ISBN: ");
        String isbn = scanner.nextLine();
        System.out.print("Publication Year: ");
        int year = scanner.nextInt();
        System.out.print("Quantity: ");
        int quantity = scanner.nextInt();
        scanner.nextLine();
        
        return new BookDetails(title, author, isbn, year, quantity);
    }

private static void updateBook() {
    if (!currentUserRole.hasPermission(Permission.MANAGE_BOOKS)) {
        System.out.println("Unauthorized access!");
        return;
    }
    
    System.out.println("\nUpdate Book");
    System.out.print("Enter Book ID to update (numeric, ObjectId, or xx-xxxx format): ");
    String input = scanner.nextLine().trim();
    
    // Determine ID type and normalize
    String bookId;
    if (input.matches("\\d+")) {  // Numeric ID
        bookId = input;
    } 
    else if (input.matches("\\d{2}-\\d{4}")) {  // xx-xxxx format
        bookId = input.replace("-", "");
    }
    else if (input.matches("[0-9a-fA-F]{24}")) {  // MongoDB ObjectId
        bookId = input;
    }
    else {
        System.out.println("Invalid ID format. Please use: numeric, ObjectId, or xx-xxxx format");
        return;
    }
    
    try {
        // First try direct lookup
        HttpRequest getRequest = createAuthenticatedRequestBuilder("/books/" + bookId)
                .GET()
                .build();
        
        HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
        handleAuthResponse(getResponse);
        
        if (getResponse.statusCode() == 404) {
            // If direct lookup fails, try search endpoint if available
            System.out.println("Book not found with direct ID lookup. Trying search...");
            HttpRequest searchRequest = createAuthenticatedRequestBuilder("/books/search?term=" + URLEncoder.encode(input, "UTF-8"))
                    .GET()
                    .build();
            
            HttpResponse<String> searchResponse = httpClient.send(searchRequest, HttpResponse.BodyHandlers.ofString());
            if (searchResponse.statusCode() == 200) {
                JsonNode results = objectMapper.readTree(searchResponse.body());
                if (results.isArray() && results.size() > 0) {
                    bookId = results.get(0).path("id").asText();
                    System.out.println("Found matching book with ID: " + bookId);
                } else {
                    System.out.println("No book found with identifier: " + input);
                    return;
                }
            } else {
                System.out.println("Book not found with identifier: " + input);
                return;
            }
        }

        JsonNode bookNode = objectMapper.readTree(getResponse.body());
        BookDetails updatedDetails = getUpdatedBookDetails(bookNode);
        
        HttpRequest putRequest = createAuthenticatedRequestBuilder("/books/" + bookId)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(updatedDetails.toJson()))
                .build();

        sendRequestAndHandleResponse(putRequest, "Book updated successfully");
    } catch (Exception e) {
        System.err.println("Error updating book: " + e.getMessage());
    }
}

    private static BookDetails getUpdatedBookDetails(JsonNode bookNode) {
        System.out.println("Current Book Details:");
        System.out.println("Title: " + bookNode.path("title").asText());
        System.out.println("Author: " + bookNode.path("author").asText());
        System.out.println("ISBN: " + bookNode.path("isbn").asText());
        System.out.println("Year: " + bookNode.path("publicationYear").asText());
        System.out.println("Quantity: " + bookNode.path("quantity").asText());
        
        System.out.println("\nEnter new details (leave blank to keep current value):");
        System.out.print("Title: ");
        String title = scanner.nextLine();
        System.out.print("Author: ");
        String author = scanner.nextLine();
        System.out.print("ISBN: ");
        String isbn = scanner.nextLine();
        System.out.print("Publication Year: ");
        String yearStr = scanner.nextLine();
        System.out.print("Quantity: ");
        String quantityStr = scanner.nextLine();
        
        return new BookDetails(
            title.isEmpty() ? bookNode.path("title").asText() : title,
            author.isEmpty() ? bookNode.path("author").asText() : author,
            isbn.isEmpty() ? bookNode.path("isbn").asText() : isbn,
            yearStr.isEmpty() ? bookNode.path("publicationYear").asInt() : Integer.parseInt(yearStr),
            quantityStr.isEmpty() ? bookNode.path("quantity").asInt() : Integer.parseInt(quantityStr)
        );
    }

    private static void memberOperations() {
        System.out.println("\n=== Member Operations ===");
        System.out.println("1. List all members");
        System.out.println("2. Add new member");
        System.out.println("3. Back to main menu");
        System.out.print("Select option: ");
        
        int choice = scanner.nextInt();
        scanner.nextLine();
        
        switch (choice) {
            case 1 -> listAllMembers();
            case 2 -> addNewMember();
            case 3 -> { return; }
            default -> System.out.println("Invalid option!");
        }
    }

    private static void listAllMembers() {
        if (!currentUserRole.hasPermission(Permission.VIEW_MEMBERS)) {
            System.out.println("Unauthorized access!");
            return;
        }
        
        HttpRequest request = createAuthenticatedRequestBuilder("/members")
                .GET()
                .build();
        
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            handleAuthResponse(response);
            
            JsonNode membersNode = objectMapper.readTree(response.body());
            if (membersNode.isArray() && membersNode.size() > 0) {
                printMembersTable(membersNode);
            } else {
                System.out.println("\nNo members registered.");
            }
        } catch (Exception e) {
            System.err.println("Error fetching members: " + e.getMessage());
        }
    }

    private static void printMembersTable(JsonNode membersNode) {
        System.out.println("\n+----------------------------- MEMBERS ------------------------------+");
        System.out.println("| ID        | Name                 | Email               | Phone       |");
        System.out.println("+-----------+----------------------+---------------------+-------------+");
        
        for (JsonNode memberNode : membersNode) {
            String fullName = memberNode.path("firstName").asText() + " " + 
                             memberNode.path("lastName").asText();
            System.out.printf("| %-9s | %-20s | %-19s | %-11s |%n",
                    shortenId(memberNode.path("id").asText()),
                    truncate(fullName, 20),
                    truncate(memberNode.path("email").asText(), 19),
                    memberNode.path("phoneNumber").asText());
        }
        System.out.println("+-----------+----------------------+---------------------+-------------+");
    }

    private static void addNewMember() {
        if (!currentUserRole.hasPermission(Permission.MANAGE_MEMBERS)) {
            System.out.println("Unauthorized access!");
            return;
        }
        
        System.out.println("\nAdd New Member");
        MemberDetails details = getMemberDetailsFromUser();
        
        HttpRequest request = createAuthenticatedRequestBuilder("/members")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(details.toJson()))
                .build();

        sendRequestAndHandleResponse(request, "Member added successfully");
    }

    private static MemberDetails getMemberDetailsFromUser() {
        System.out.print("First Name: ");
        String firstName = scanner.nextLine();
        System.out.print("Last Name: ");
        String lastName = scanner.nextLine();
        System.out.print("Email: ");
        String email = scanner.nextLine();
        System.out.print("Phone Number: ");
        String phone = scanner.nextLine();
        System.out.print("Address: ");
        String address = scanner.nextLine();
        
        return new MemberDetails(firstName, lastName, email, phone, address);
    }

    private static void loanOperations() {
        System.out.println("\n=== Loan Operations ===");
        System.out.println("1. List all loans");
        System.out.println("2. Create new loan");
        System.out.println("3. Return a book");
        System.out.println("4. Back to main menu");
        System.out.print("Select option: ");
        
        int choice = scanner.nextInt();
        scanner.nextLine();
        
        switch (choice) {
            case 1 -> listAllLoans();
            case 2 -> createNewLoan();
            case 3 -> returnBook();
            case 4 -> { return; }
            default -> System.out.println("Invalid option!");
        }
    }

    private static void listAllLoans() {
        if (!currentUserRole.hasPermission(Permission.MANAGE_LOANS)) {
            System.out.println("Unauthorized access!");
            return;
        }
    
        try {
            HttpRequest request = createAuthenticatedRequestBuilder("/loans")
                    .GET()
                    .build();
    
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    
            // Handle potential authentication issues
            if (response.statusCode() == 401) {
                handleUnauthorizedResponse();
                return;
            }
    
            if (response.statusCode() == 200) {
                try {
                    JsonNode responseNode = objectMapper.readTree(response.body());
                    // Handle both direct array and paginated responses
                    JsonNode loansNode = responseNode.has("content") ? responseNode.path("content") : responseNode;
                    
                    if (loansNode.isArray()) {
                        if (loansNode.size() > 0) {
                            printLoansTable(loansNode);
                        } else {
                            System.out.println("\nNo loans found in the system.");
                        }
                    } else {
                        System.err.println("Server returned unexpected format. Expected array of loans.");
                        System.err.println("Response: " + response.body());
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing server response: " + e.getMessage());
                }
            } else {
                System.err.println("Failed to fetch loans. Server returned: " + response.statusCode());
                System.err.println("Response: " + response.body());
            }
        } catch (IOException e) {
            System.err.println("Network error: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Operation interrupted");
            Thread.currentThread().interrupt();
        }
    }
    
    private static void printLoansTable(JsonNode loansNode) {
        System.out.println("\n+-------------------------------- ACTIVE LOANS ------------------------------+");
        System.out.println("| Loan ID   | Book Title           | Member Name        | Loan Date  | Due Date  |");
        System.out.println("+-----------+----------------------+--------------------+------------+-----------+");
        
        for (JsonNode loanNode : loansNode) {
            // Handle both nested and flat response structures
            String bookTitle = loanNode.has("book") 
                ? loanNode.path("book").path("title").asText()
                : loanNode.path("bookTitle").asText("Unknown");
                
            String memberName = loanNode.has("member")
                ? loanNode.path("member").path("firstName").asText() + " " + 
                  loanNode.path("member").path("lastName").asText()
                : loanNode.path("memberName").asText("Unknown");
    
            System.out.printf("| %-9s | %-20s | %-18s | %-10s | %-9s |%n",
                    shortenId(loanNode.path("id").asText()),
                    truncate(bookTitle, 20),
                    truncate(memberName, 18),
                    loanNode.path("loanDate").asText().substring(0, 10), // Just show date part
                    loanNode.path("dueDate").asText().substring(0, 10));
        }
        System.out.println("+-----------+----------------------+--------------------+------------+-----------+");
        
        // Display additional loan count information
        System.out.println("Total loans displayed: " + loansNode.size());
    }
    private static void createNewLoan() {
        if (!currentUserRole.hasPermission(Permission.MANAGE_LOANS)) {
            System.out.println("Unauthorized access!");
            return;
        }
    
        System.out.println("\n=== CREATE NEW LOAN ===");
        
        try {
            // Get Book ID
            String bookId;
            while (true) {
                System.out.print("Enter Book ID (or 'cancel' to abort): ");
                String input = scanner.nextLine().trim();
                
                if (input.equalsIgnoreCase("cancel")) {
                    System.out.println("Loan creation cancelled.");
                    return;
                }
                
                bookId = normalizeId(input);
                if (bookId != null) break;
                System.out.println("Invalid format. Please use numeric, ObjectId, or xx-xxxx format.");
            }
    
            // Get Member ID
            String memberId;
            while (true) {
                System.out.print("Enter Member ID (or 'cancel' to abort): ");
                String input = scanner.nextLine().trim();
                
                if (input.equalsIgnoreCase("cancel")) {
                    System.out.println("Loan creation cancelled.");
                    return;
                }
                
                memberId = normalizeId(input);
                if (memberId != null) break;
                System.out.println("Invalid format. Please use numeric, ObjectId, or xx-xxxx format.");
            }
    
            // Build loan request
            String loanJson = String.format(
                "{\"bookId\":\"%s\",\"memberId\":\"%s\",\"loanDate\":\"%s\",\"dueDate\":\"%s\"}", 
                bookId, memberId, LocalDate.now(), LocalDate.now().plusDays(14));
    
            HttpRequest request = createAuthenticatedRequestBuilder("/loans")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(loanJson))
                    .build();
    
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    
            // Handle response
            switch (response.statusCode()) {
                case 200, 201 -> {  // Handle both 200 and 201 as success
                    JsonNode loanNode = objectMapper.readTree(response.body());
                    System.out.println("\nLoan created successfully!");
                    
                    // Print single loan in table format without creating an array
                    System.out.println("\n+-------------------------------- ACTIVE LOANS ------------------------------+");
                    System.out.println("| Loan ID   | Book Title           | Member Name        | Loan Date  | Due Date  |");
                    System.out.println("+-----------+----------------------+--------------------+------------+-----------+");
                    
                    String bookTitle = loanNode.has("book") 
                        ? loanNode.path("book").path("title").asText()
                        : loanNode.path("bookTitle").asText("Unknown");
                        
                    String memberName = loanNode.has("member")
                        ? loanNode.path("member").path("firstName").asText() + " " + 
                          loanNode.path("member").path("lastName").asText()
                        : loanNode.path("memberName").asText("Unknown");
    
                    System.out.printf("| %-9s | %-20s | %-18s | %-10s | %-9s |%n",
                            shortenId(loanNode.path("id").asText()),
                            truncate(bookTitle, 20),
                            truncate(memberName, 18),
                            loanNode.path("loanDate").asText().substring(0, 10),
                            loanNode.path("dueDate").asText().substring(0, 10));
                    
                    System.out.println("+-----------+----------------------+--------------------+------------+-----------+");
                }
                case 400 -> {
                    System.err.println("Validation error: " + response.body());
                    try {
                        JsonNode errorNode = objectMapper.readTree(response.body());
                        System.err.println("Details: " + errorNode.path("message").asText());
                    } catch (Exception e) {
                        System.err.println("Raw response: " + response.body());
                    }
                }
                case 404 -> System.err.println("Error: Book or member not found");
                case 409 -> System.err.println("Error: " + response.body());  // Conflict (book not available)
                case 401 -> handleUnauthorizedResponse();
                default -> {
                    System.err.println("Unexpected server response: " + response.statusCode());
                    System.err.println("Response: " + response.body());
                }
            }
        } catch (IOException e) {
            System.err.println("Network error: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Operation interrupted");
            Thread.currentThread().interrupt();
        }
    }
    
    private static void handleUnauthorizedResponse() {
        System.err.println("Authentication failed. Possible reasons:");
        System.err.println("1. Your session has expired");
        System.err.println("2. Invalid credentials");
        System.err.println("3. Server configuration issue");
        System.out.println("Please try logging in again.");
    }
// Enhanced printLoansTable method
private static void printLoansTable(List<JsonNode> loans) {
    System.out.println("+------------+----------------------+---------------------+------------+------------+");
    System.out.println("| Loan ID    | Book Title           | Member Name         | Loan Date  | Due/Return |");
    System.out.println("+------------+----------------------+---------------------+------------+------------+");
    
    for (JsonNode loan : loans) {
        boolean isReturned = loan.path("returned").asBoolean();
        String dateColumn = isReturned ? 
            loan.path("returnDate").asText() : 
            loan.path("dueDate").asText();
        
        System.out.printf("| %-10s | %-20s | %-19s | %-10s | %-10s |\n",
            shortenId(loan.path("id").asText()),
            truncate(loan.path("bookTitle").asText(), 20),
            truncate(loan.path("memberName").asText(), 19),
            loan.path("loanDate").asText(),
            dateColumn);
    }
    System.out.println("+------------+----------------------+---------------------+------------+------------+");
}

// Helper method to check token expiration
private static boolean isTokenExpired() {
    // Implement your token expiration logic here
    // For example, check if jwtToken is null or expired
    return jwtToken == null; // Simple check - enhance as needed
}
  


    // Helper method to normalize different ID formats
    private static String normalizeId(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        
        if (input.matches("\\d+")) {  // Numeric ID
            return input;
        } 
        else if (input.matches("\\d{2}-\\d{4}")) {  // xx-xxxx format
            return input.replace("-", "");
        }
        else if (input.matches("[0-9a-fA-F]{24}")) {  // MongoDB ObjectId
            return input;
        }
        
        return null;
    }


    private static void userManagement() {
        if (!currentUserRole.hasPermission(Permission.MANAGE_USERS)) {
            System.out.println("Unauthorized access!");
            return;
        }
        
        System.out.println("\n=== User Management ===");
        System.out.println("1. List all users");
        System.out.println("2. Add new user");
        System.out.println("3. Change password");
        System.out.println("4. Back to main menu");
        System.out.print("Select option: ");
        
        int choice = scanner.nextInt();
        scanner.nextLine();
        
        switch (choice) {
            case 1 -> listAllUsers();
            case 2 -> addNewUser();
            case 3 -> changePassword();
            case 4 -> { return; }
            default -> System.out.println("Invalid option!");
        }
    }

    private static void listAllUsers() {
        HttpRequest request = createAuthenticatedRequestBuilder("/users")
                .GET()
                .build();
        
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            handleAuthResponse(response);
            
            JsonNode usersNode = objectMapper.readTree(response.body());
            if (usersNode.isArray() && usersNode.size() > 0) {
                printUsersTable(usersNode);
            } else {
                System.out.println("\nNo users found.");
            }
        } catch (Exception e) {
            System.err.println("Error fetching users: " + e.getMessage());
        }
    }

    private static void printUsersTable(JsonNode usersNode) {
        System.out.println("\n+----------------------------- USERS ------------------------------+");
        System.out.println("| ID        | Username             | Role                 |");
        System.out.println("+-----------+----------------------+----------------------+");
        
        for (JsonNode userNode : usersNode) {
            System.out.printf("| %-9s | %-20s | %-20s |%n",
                    shortenId(userNode.path("id").asText()),
                    truncate(userNode.path("username").asText(), 20),
                    userNode.path("role").asText());
        }
        System.out.println("+-----------+----------------------+----------------------+");
    }

    private static void addNewUser() {
        System.out.println("\nAdd New User");
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        System.out.print("Role (ADMIN/LIBRARIAN/ASSISTANT): ");
        String role = scanner.nextLine().toUpperCase();
        
        String userJson = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\",\"role\":\"%s\"}",
                username, password, role);
        
        HttpRequest request = createAuthenticatedRequestBuilder("/users")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(userJson))
                .build();

        sendRequestAndHandleResponse(request, "User added successfully");
    }

    private static void changePassword() {
        System.out.println("\nChange Password");
        System.out.print("Current Password: ");
        String currentPassword = scanner.nextLine();
        System.out.print("New Password: ");
        String newPassword = scanner.nextLine();
        
        String passwordJson = String.format(
                "{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}",
                currentPassword, newPassword);
        
        HttpRequest request = createAuthenticatedRequestBuilder("/users/change-password")
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(passwordJson))
                .build();

        sendRequestAndHandleResponse(request, "Password changed successfully");
    }

    private static HttpRequest.Builder createAuthenticatedRequestBuilder(String endpoint) {
        return HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + jwtToken);
    }

    private static void sendRequestAndHandleResponse(HttpRequest request, String successMessage) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            handleAuthResponse(response);
            System.out.println(successMessage + ": " + response.body());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static void handleAuthResponse(HttpResponse<String> response) {
        if (response.statusCode() == 401) {
            System.out.println("Session expired. Please login again.");
            exitApplication();
        } else if (response.statusCode() == 403) {
            System.out.println("Unauthorized access!");
            exitApplication();
        }
    }

    private static void exitApplication() {
        scanner.close();
        System.exit(0);
    }

    private static String truncate(String text, int length) {
        if (text == null) return "";
        if (text.length() <= length) return text;
        return text.substring(0, length - 3) + "...";
    }

    private static String shortenId(String id) {
        if (id == null || id.length() <= 8) return id;
        return id.substring(0, 4) + "..." + id.substring(id.length() - 4);
    }

    private static class BookDetails {
        private final String title;
        private final String author;
        private final String isbn;
        private final int publicationYear;
        private final int quantity;

        public BookDetails(String title, String author, String isbn, int publicationYear, int quantity) {
            this.title = title;
            this.author = author;
            this.isbn = isbn;
            this.publicationYear = publicationYear;
            this.quantity = quantity;
        }

        public String toJson() {
            return String.format(
                "{\"title\":\"%s\",\"author\":\"%s\",\"isbn\":\"%s\",\"publicationYear\":%d,\"quantity\":%d}",
                title, author, isbn, publicationYear, quantity);
        }
    }

    private static class MemberDetails {
        private final String firstName;
        private final String lastName;
        private final String email;
        private final String phoneNumber;
        private final String address;

        public MemberDetails(String firstName, String lastName, String email, String phoneNumber, String address) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.phoneNumber = phoneNumber;
            this.address = address;
        }

        public String toJson() {
            return String.format(
                "{\"firstName\":\"%s\",\"lastName\":\"%s\",\"email\":\"%s\",\"phoneNumber\":\"%s\",\"address\":\"%s\"}",
                firstName, lastName, email, phoneNumber, address);
        }
    }


    private static void returnBook() {
        if (!currentUserRole.hasPermission(Permission.MANAGE_LOANS)) {
            System.out.println("Unauthorized access!");
            return;
        }
    
        try {
            // Fetch all loans (we'll filter active ones client-side)
            HttpRequest listRequest = createAuthenticatedRequestBuilder("/loans")
                    .GET()
                    .build();
            
            HttpResponse<String> listResponse = httpClient.send(listRequest, HttpResponse.BodyHandlers.ofString());
    
            if (listResponse.statusCode() != 200) {
                System.err.println("Error fetching loans: " + listResponse.body());
                return;
            }
    
            JsonNode responseNode = objectMapper.readTree(listResponse.body());
            JsonNode loansNode = responseNode.has("content") ? responseNode.path("content") : responseNode;
            
            if (!loansNode.isArray() || loansNode.size() == 0) {
                System.out.println("\nNo loans found in the system.");
                return;
            }
    
            // Filter active loans
            List<JsonNode> activeLoans = new ArrayList<>();
            for (JsonNode loan : loansNode) {
                if (!loan.path("returned").asBoolean()) {
                    activeLoans.add(loan);
                }
            }
    
            if (activeLoans.isEmpty()) {
                System.out.println("\nNo active loans available for return.");
                return;
            }
    
            // Display active loans with removal option
            boolean continueProcessing = true;
            while (continueProcessing && !activeLoans.isEmpty()) {
                System.out.println("\n=== ACTIVE LOANS ===");
                System.out.println("+----+----------------------+---------------------+------------+------------+");
                System.out.println("| #  | Book Title           | Member Name         | Loan Date  | Due Date   |");
                System.out.println("+----+----------------------+---------------------+------------+------------+");
                
                for (int i = 0; i < activeLoans.size(); i++) {
                    JsonNode loan = activeLoans.get(i);
                    String bookTitle = loan.has("book") 
                        ? loan.path("book").path("title").asText()
                        : loan.path("bookTitle").asText("Unknown");
                        
                    String memberName = loan.has("member")
                        ? loan.path("member").path("firstName").asText() + " " + 
                          loan.path("member").path("lastName").asText()
                        : loan.path("memberName").asText("Unknown");
    
                    System.out.printf("| %-2d | %-20s | %-19s | %-10s | %-10s |%n",
                            i + 1,
                            truncate(bookTitle, 20),
                            truncate(memberName, 19),
                            loan.path("loanDate").asText().substring(0, 10),
                            loan.path("dueDate").asText().substring(0, 10));
                }
                System.out.println("+----+----------------------+---------------------+------------+------------+");
    
                System.out.println("\nOptions:");
                System.out.println("1. Return a book");
                System.out.println("2. Remove a book from active loans");
                System.out.println("3. Cancel");
                System.out.print("Select option: ");
                
                String option = scanner.nextLine().trim();
                
                switch (option) {
                    case "1": // Return a book
                        System.out.print("\nEnter loan number to return (1-" + activeLoans.size() + "): ");
                        String returnInput = scanner.nextLine().trim();
                        
                        try {
                            int returnIndex = Integer.parseInt(returnInput) - 1;
                            if (returnIndex < 0 || returnIndex >= activeLoans.size()) {
                                System.out.println("Invalid selection.");
                                continue;
                            }
    
                            JsonNode selectedLoan = activeLoans.get(returnIndex);
                            String loanId = selectedLoan.path("id").asText();
                            String bookTitle = selectedLoan.has("book") 
                                ? selectedLoan.path("book").path("title").asText()
                                : selectedLoan.path("bookTitle").asText();
                            String memberName = selectedLoan.has("member")
                                ? selectedLoan.path("member").path("firstName").asText() + " " + 
                                  selectedLoan.path("member").path("lastName").asText()
                                : selectedLoan.path("memberName").asText();
    
                            // Process return
                            String returnJson = String.format(
                                "{\"returned\":true,\"returnDate\":\"%s\"}",
                                LocalDate.now());
    
                            HttpRequest returnRequest = createAuthenticatedRequestBuilder("/loans/" + loanId + "/return")
                                    .header("Content-Type", "application/json")
                                    .PUT(HttpRequest.BodyPublishers.ofString(returnJson))
                                    .build();
    
                            HttpResponse<String> returnResponse = httpClient.send(returnRequest, HttpResponse.BodyHandlers.ofString());
    
                            if (returnResponse.statusCode() == 200) {
                                System.out.println("\nBook returned successfully!");
                                activeLoans.remove(returnIndex); // Remove from active list
                            } else {
                                System.err.println("Error returning book: " + returnResponse.body());
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid input. Please enter a number.");
                        }
                        break;
                        
                    case "2": // Remove from active loans
                        System.out.print("\nEnter loan number to remove (1-" + activeLoans.size() + "): ");
                        String removeInput = scanner.nextLine().trim();
                        
                        try {
                            int removeIndex = Integer.parseInt(removeInput) - 1;
                            if (removeIndex < 0 || removeIndex >= activeLoans.size()) {
                                System.out.println("Invalid selection.");
                                continue;
                            }
    
                            JsonNode removedLoan = activeLoans.get(removeIndex);
                            String bookTitle = removedLoan.has("book") 
                                ? removedLoan.path("book").path("title").asText()
                                : removedLoan.path("bookTitle").asText();
                            
                            activeLoans.remove(removeIndex);
                            System.out.println("\nRemoved '" + bookTitle + "' from active loans list.");
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid input. Please enter a number.");
                        }
                        break;
                        
                    case "3": // Cancel
                        continueProcessing = false;
                        System.out.println("Operation cancelled.");
                        break;
                        
                    default:
                        System.out.println("Invalid option. Please try again.");
                }
            }
        } catch (IOException e) {
            System.err.println("Network error: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Operation interrupted");
            Thread.currentThread().interrupt();
        }
    }
}