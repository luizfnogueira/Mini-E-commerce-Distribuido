package com.example.orders.controller;

import com.example.orders.dto.OrderRequest;
import com.example.orders.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class OrderController {
    private final OrderRepository repository;

    public OrderController(OrderRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/orders")
    public ResponseEntity<?> create(
            @RequestBody OrderRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String authenticatedUserId,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) throws Exception {
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
            @RequestHeader(value = "X-User-Id", required = false) String authenticatedUserId,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) throws Exception {
        if (!"admin".equals(role) && !String.valueOf(userId).equals(authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "acesso negado"));
        }
        return ResponseEntity.ok(repository.findByUserId(userId));
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
