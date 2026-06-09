package com.example.orders.dto;

public record OrderResponse(long id, long userId, long productId, int quantity, String createdAt) {
}
