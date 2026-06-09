package com.example.products.dto;

import java.math.BigDecimal;

public record ProductRequest(String name, String description, BigDecimal price, int stock) {
}
