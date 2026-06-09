package com.example.products.dto;

import java.math.BigDecimal;

public record ProductResponse(long id, String name, String description, BigDecimal price, int stock) {
}
