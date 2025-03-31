package com.library.config;

import com.library.model.User;
import com.library.repository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtGenerator {
    private static final Logger logger = LoggerFactory.getLogger(JwtGenerator.class);
    
    private final SecretKey jwtSecretKey;
    private final int jwtExpirationMs;
    private final String jwtIssuer;
    private final UserRepository userRepository;

    public JwtGenerator(
            @Value("${jwt.secret:}") String jwtSecret,
            @Value("${jwt.expiration.ms}") int jwtExpirationMs,
            @Value("${jwt.issuer}") String jwtIssuer,
            UserRepository userRepository) {
        
        this.jwtSecretKey = initializeSecretKey(jwtSecret);
        this.jwtExpirationMs = jwtExpirationMs;
        this.jwtIssuer = jwtIssuer;
        this.userRepository = userRepository;
        
        logger.info("JWT configuration initialized - Issuer: {}, Expiration: {} ms", 
            jwtIssuer, jwtExpirationMs);
    }

    private SecretKey initializeSecretKey(String configuredSecret) {
        if (configuredSecret != null && !configuredSecret.isBlank()) {
            return Keys.hmacShaKeyFor(configuredSecret.getBytes(StandardCharsets.UTF_8));
        }
        
        SecretKey generatedKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        logger.warn("No JWT secret configured - Using dynamically generated key. "
                  + "This is not suitable for production!");
        return generatedKey;
    }

    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = resolveUserPrincipal(authentication);
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .setIssuer(jwtIssuer)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(jwtSecretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    private UserPrincipal resolveUserPrincipal(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof UserPrincipal) {
            return (UserPrincipal) principal;
        } 
        else if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;
            User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException(
                    "User not found: " + userDetails.getUsername()));
            return new UserPrincipal(user);
        }
        
        throw new IllegalStateException("Unsupported principal type: " + principal.getClass());
    }

    public String getUsernameFromJWT(String token) {
        return parseToken(token).getBody().getSubject();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (SignatureException ex) {
            logger.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty");
        }
        return false;
    }

    private Jws<Claims> parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(jwtSecretKey)
                .build()
                .parseClaimsJws(token);
    }

    public String refreshToken(String token) {
        Claims claims = parseToken(token).getBody();
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtExpirationMs);
        
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(jwtSecretKey, SignatureAlgorithm.HS512)
                .compact();
    }
}