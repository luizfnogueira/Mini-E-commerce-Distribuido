package com.example.users.controller;

import com.example.users.dto.LoginRequest;
import com.example.users.dto.LoginResponse;
import com.example.users.dto.RegisterRequest;
import com.example.users.dto.UserResponse;
import com.example.users.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@RestController
public class UserController {
    private final UserRepository repository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecretKey jwtKey;
    private final long expirationMinutes;

    public UserController(
            UserRepository repository,
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-minutes}") long expirationMinutes
    ) {
        this.repository = repository;
        this.jwtKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    @PostMapping("/users/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) throws Exception {
        if (isBlank(request.name()) || isBlank(request.email()) || isBlank(request.password())) {
            return ResponseEntity.badRequest().body(Map.of("error", "name, email e password sao obrigatorios"));
        }

        String role = "admin".equalsIgnoreCase(request.role()) ? "admin" : "user";
        String hash = passwordEncoder.encode(request.password());
        UserResponse user = repository.create(request.name(), request.email(), hash, role);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PostMapping("/users/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) throws Exception {
        var user = repository.findByEmail(request.email());
        if (user.isEmpty() || !passwordEncoder.matches(request.password(), user.get().passwordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "credenciais invalidas"));
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationMinutes * 60);
        String token = Jwts.builder()
                .subject(String.valueOf(user.get().id()))
                .claim("userId", user.get().id())
                .claim("email", user.get().email())
                .claim("role", user.get().role())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(jwtKey)
                .compact();

        return ResponseEntity.ok(new LoginResponse(token));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> profile(
            @PathVariable long id,
            @RequestHeader(value = "X-User-Id", required = false) String authenticatedUserId,
            @RequestHeader(value = "X-User-Role", required = false) String authenticatedRole
    ) throws Exception {
        if (!"admin".equals(authenticatedRole) && !String.valueOf(id).equals(authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "acesso negado"));
        }

        return repository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "usuario nao encontrado")));
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
