package com.example.orders.dto;

public record OrderRequest(long userId, long productId, int quantity) {
}
