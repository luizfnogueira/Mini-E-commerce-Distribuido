package com.example.users.dto;

public record RegisterRequest(String name, String email, String password, String role) {
}
