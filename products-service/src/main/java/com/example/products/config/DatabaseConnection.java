package com.example.products.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Component
public class DatabaseConnection {
    private static final String DB1 = "jdbc:sqlite:products_1.db";
    private static final String DB2 = "jdbc:sqlite:products_2.db";

    public Connection getReplicaOne() throws SQLException {
        return DriverManager.getConnection(DB1);
    }

    public Connection getReplicaTwo() throws SQLException {
        return DriverManager.getConnection(DB2);
    }

    @PostConstruct
    public void initialize() throws SQLException {
        createTable(getReplicaOne());
        createTable(getReplicaTwo());
    }

    private void createTable(Connection connection) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS products (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    description TEXT,
                    price REAL NOT NULL,
                    stock INTEGER NOT NULL DEFAULT 0,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """;

        try (connection; PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }
}
