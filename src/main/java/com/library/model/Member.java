package com.library.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.*;

@Data
@Document(collection = "members")
public class Member {
    @Id
    private String id;
    
    @NotBlank(message = "Name is required")
    private String name;
    
    @Email(message = "Email should be valid")
    private String email;
    
    @Pattern(regexp = "^\\d{10}$", message = "Phone number must be 10 digits")
    private String phone;
    
    @NotBlank(message = "Address is required")
    private String address;
    
    private String membershipDate;

    // Add this helper method for the loan service
    public String getFullName() {
        return this.name;
    }
}