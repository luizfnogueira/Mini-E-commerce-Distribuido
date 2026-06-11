package com.example.products.controller;

import com.example.products.dto.ProductRequest;
import com.example.products.repository.ProductRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
public class ProductController {
    private final ProductRepository repository;
    private final SecretKey jwtKey;

    public ProductController(ProductRepository repository, @Value("${jwt.secret}") String secret) {
        this.repository = repository;
        this.jwtKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/products")
    public ResponseEntity<?> create(
            @RequestBody ProductRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) throws Exception {
        Claims claims = validateToken(authorization);
        if (claims == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "token invalido ou ausente"));
        }
        if (!"admin".equals(claims.get("role", String.class))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "apenas admin pode criar produtos"));
        }
        if (request.name() == null || request.name().isBlank() || request.price() == null || request.price().compareTo(BigDecimal.ZERO) < 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "name e price valido sao obrigatorios"));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(repository.create(request));
    }

    @GetMapping("/products")
    public ResponseEntity<?> list() throws Exception {
        return ResponseEntity.ok(repository.findAllRoundRobin());
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> detail(@PathVariable long id) throws Exception {
        return repository.findByIdRoundRobin(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "produto nao encontrado")));
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    private Claims validateToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        try {
            return Jwts.parser()
                    .verifyWith(jwtKey)
                    .build()
                    .parseSignedClaims(authorization.substring(7))
                    .getPayload();
        } catch (Exception error) {
            return null;
        }
    }
}
