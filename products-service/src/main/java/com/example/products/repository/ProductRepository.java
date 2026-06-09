package com.example.products.repository;

import com.example.products.config.DatabaseConnection;
import com.example.products.dto.ProductRequest;
import com.example.products.dto.ProductResponse;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Repository
public class ProductRepository {
    private final DatabaseConnection database;
    private final AtomicInteger roundRobin = new AtomicInteger();

    public ProductRepository(DatabaseConnection database) {
        this.database = database;
    }

    public ProductResponse create(ProductRequest request) throws Exception {
        Connection replicaOne = database.getReplicaOne();
        Connection replicaTwo = database.getReplicaTwo();
        try (replicaOne; replicaTwo) {
            replicaOne.setAutoCommit(false);
            replicaTwo.setAutoCommit(false);

            ProductResponse product = insertReplicaOne(replicaOne, request);
            insertReplicaTwo(replicaTwo, product);

            replicaOne.commit();
            replicaTwo.commit();
            return product;
        } catch (Exception error) {
            rollbackQuietly(replicaOne);
            rollbackQuietly(replicaTwo);
            throw error;
        }
    }

    public List<ProductResponse> findAllRoundRobin() throws Exception {
        try (Connection connection = nextReplica()) {
            String sql = "SELECT id, name, description, price, stock FROM products ORDER BY id";
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rs = statement.executeQuery()) {
                List<ProductResponse> products = new ArrayList<>();
                while (rs.next()) {
                    products.add(map(rs));
                }
                return products;
            }
        }
    }

    public Optional<ProductResponse> findByIdRoundRobin(long id) throws Exception {
        try (Connection connection = nextReplica()) {
            String sql = "SELECT id, name, description, price, stock FROM products WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, id);
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(map(rs));
                }
            }
        }
    }

    private ProductResponse insertReplicaOne(Connection connection, ProductRequest request) throws Exception {
        String sql = "INSERT INTO products (name, description, price, stock) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, request.name());
            statement.setString(2, request.description());
            statement.setBigDecimal(3, request.price());
            statement.setInt(4, request.stock());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return new ProductResponse(keys.getLong(1), request.name(), request.description(), request.price(), request.stock());
            }
        }
    }

    private void insertReplicaTwo(Connection connection, ProductResponse product) throws Exception {
        String sql = "INSERT INTO products (id, name, description, price, stock) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, product.id());
            statement.setString(2, product.name());
            statement.setString(3, product.description());
            statement.setBigDecimal(4, product.price());
            statement.setInt(5, product.stock());
            statement.executeUpdate();
        }
    }

    private Connection nextReplica() throws Exception {
        int count = roundRobin.getAndIncrement();
        return count % 2 == 0 ? database.getReplicaOne() : database.getReplicaTwo();
    }

    private ProductResponse map(ResultSet rs) throws Exception {
        return new ProductResponse(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                BigDecimal.valueOf(rs.getDouble("price")),
                rs.getInt("stock")
        );
    }

    private void rollbackQuietly(Connection connection) {
        try {
            if (connection != null) {
                connection.rollback();
            }
        } catch (Exception ignored) {
        }
    }
}
