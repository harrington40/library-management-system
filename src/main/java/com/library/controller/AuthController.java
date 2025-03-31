package com.library.controller;

import com.library.config.JwtGenerator;
import com.library.config.UserPrincipal;
import com.library.payload.request.LoginRequest;
import com.library.payload.response.JwtResponse;
import com.library.repository.UserRepository;
import com.library.service.TokenService;
import com.library.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtGenerator jwtGenerator;
    private final UserRepository userRepository;
    private final UserService userService;
    private final TokenService tokenService;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager,
                           JwtGenerator jwtGenerator,
                           UserRepository userRepository,
                           UserService userService,
                           TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.jwtGenerator = jwtGenerator;
        this.userRepository = userRepository;
        this.userService = userService;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtGenerator.generateToken(authentication);
            UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();

            // Safely get the first authority
            String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_USER"); // default role if none found

            // Return JWT response with username and role
            return ResponseEntity.ok(new JwtResponse(jwt, userDetails.getUsername(), role));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                        "error", "Invalid credentials",
                        "timestamp", System.currentTimeMillis()
                    ));
        }
    }
}
