package com.library.service;


import com.library.model.User;
import com.library.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

public interface UserService {
    List<User> getAllUsers();
    User getUserById(String id);
    User createUser(User user);
    User updateUser(String id, User user);
    void deleteUser(String id);
}


