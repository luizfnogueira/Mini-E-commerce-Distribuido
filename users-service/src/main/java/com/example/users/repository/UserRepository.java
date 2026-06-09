package com.example.users.repository;

import com.example.users.config.DatabaseConnection;
import com.example.users.dto.UserResponse;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;

@Repository
public class UserRepository {
    private final DatabaseConnection database;

    public UserRepository(DatabaseConnection database) {
        this.database = database;
    }

    public UserResponse create(String name, String email, String passwordHash, String role) throws Exception {
        String sql = "INSERT INTO users (name, email, password_hash, role) VALUES (?, ?, ?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.setString(2, email);
            statement.setString(3, passwordHash);
            statement.setString(4, role);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return new UserResponse(keys.getLong(1), name, email, role);
            }
        }
    }

    public Optional<UserRecord> findByEmail(String email) throws Exception {
        String sql = "SELECT id, name, email, password_hash, role FROM users WHERE email = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new UserRecord(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("role")
                ));
            }
        }
    }

    public Optional<UserResponse> findById(long id) throws Exception {
        String sql = "SELECT id, name, email, role FROM users WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new UserResponse(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("role")
                ));
            }
        }
    }

    public record UserRecord(long id, String name, String email, String passwordHash, String role) {
    }
}
