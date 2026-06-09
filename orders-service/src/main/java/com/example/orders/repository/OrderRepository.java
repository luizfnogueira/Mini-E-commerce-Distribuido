package com.example.orders.repository;

import com.example.orders.config.DatabaseConnection;
import com.example.orders.dto.OrderRequest;
import com.example.orders.dto.OrderResponse;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Repository
public class OrderRepository {
    private final DatabaseConnection database;

    public OrderRepository(DatabaseConnection database) {
        this.database = database;
    }

    public OrderResponse create(OrderRequest request) throws Exception {
        String sql = "INSERT INTO orders (user_id, product_id, quantity) VALUES (?, ?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, request.userId());
            statement.setLong(2, request.productId());
            statement.setInt(3, Math.max(request.quantity(), 1));
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return findById(keys.getLong(1));
            }
        }
    }

    public List<OrderResponse> findByUserId(long userId) throws Exception {
        String sql = "SELECT id, user_id, product_id, quantity, created_at FROM orders WHERE user_id = ? ORDER BY id DESC";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                List<OrderResponse> orders = new ArrayList<>();
                while (rs.next()) {
                    orders.add(map(rs));
                }
                return orders;
            }
        }
    }

    private OrderResponse findById(long id) throws Exception {
        String sql = "SELECT id, user_id, product_id, quantity, created_at FROM orders WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return map(rs);
            }
        }
    }

    private OrderResponse map(ResultSet rs) throws Exception {
        return new OrderResponse(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getLong("product_id"),
                rs.getInt("quantity"),
                rs.getString("created_at")
        );
    }
}
