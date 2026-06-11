package com.example.orders.controller;

import com.example.orders.dto.OrderRequest;
import com.example.orders.repository.OrderRepository;
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
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
public class OrderController {
    private final OrderRepository repository;
    private final SecretKey jwtKey;

    public OrderController(OrderRepository repository, @Value("${jwt.secret}") String secret) {
        this.repository = repository;
        this.jwtKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/orders")
    public ResponseEntity<?> create(
            @RequestBody OrderRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) throws Exception {
        Claims claims = validateToken(authorization);
        if (claims == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "token invalido ou ausente"));
        }

        String authenticatedUserId = String.valueOf(claims.get("userId"));
        String role = claims.get("role", String.class);
        if (!"admin".equals(role) && !String.valueOf(request.userId()).equals(authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "usuario so pode criar pedidos para si mesmo"));
        }
        if (request.userId() <= 0 || request.productId() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId e productId sao obrigatorios"));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(repository.create(request));
    }

    @GetMapping("/orders/{userId}")
    public ResponseEntity<?> list(
            @PathVariable long userId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) throws Exception {
        Claims claims = validateToken(authorization);
        if (claims == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "token invalido ou ausente"));
        }

        String authenticatedUserId = String.valueOf(claims.get("userId"));
        String role = claims.get("role", String.class);
        if (!"admin".equals(role) && !String.valueOf(userId).equals(authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "acesso negado"));
        }
        return ResponseEntity.ok(repository.findByUserId(userId));
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
