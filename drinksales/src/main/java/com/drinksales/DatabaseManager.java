package com.drinksales;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String URL = "jdbc:h2:~/KINYWAJI";
    private static final String USER = "sa";
    private static final String PASSWORD = "";
    private static final String SCHEMA_SQL = """
        CREATE TABLE IF NOT EXISTS branches (
            id INT PRIMARY KEY AUTO_INCREMENT,
            name VARCHAR(255) NOT NULL
        );
        CREATE TABLE IF NOT EXISTS drinks (
            id INT PRIMARY KEY AUTO_INCREMENT,
            name VARCHAR(255) NOT NULL,
            price DECIMAL(10, 2) NOT NULL
        );
        CREATE TABLE IF NOT EXISTS customers (
            id INT PRIMARY KEY AUTO_INCREMENT,
            name VARCHAR(255) NOT NULL
        );
        CREATE TABLE IF NOT EXISTS orders (
            id INT PRIMARY KEY AUTO_INCREMENT,
            customer_id INT,
            branch_id INT,
            drink_id INT,
            quantity INT NOT NULL,
            order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (customer_id) REFERENCES customers(id),
            FOREIGN KEY (branch_id) REFERENCES branches(id),
            FOREIGN KEY (drink_id) REFERENCES drinks(id)
        );
        CREATE TABLE IF NOT EXISTS stock (
            branch_id INT,
            drink_id INT,
            quantity INT NOT NULL,
            PRIMARY KEY (branch_id, drink_id),
            FOREIGN KEY (branch_id) REFERENCES branches(id),
            FOREIGN KEY (drink_id) REFERENCES drinks(id)
        );
        """;
    private static final String DATA_SQL = """
        MERGE INTO branches (id, name) KEY(id) VALUES (1, 'Nairobi'), (2, 'Nakuru'), (3, 'Mombasa'), (4, 'Kisumu');
        MERGE INTO drinks (id, name, price) KEY(id) VALUES (1, 'Cola', 1.50), (2, 'Fanta', 1.50), (3, 'Sprite', 1.50), (4, 'Water', 1.00);
        MERGE INTO stock (branch_id, drink_id, quantity) KEY(branch_id, drink_id) VALUES 
            (1, 1, 100), (1, 2, 100), (1, 3, 100), (1, 4, 100),
            (2, 1, 100), (2, 2, 100), (2, 3, 100), (2, 4, 100),
            (3, 1, 100), (3, 2, 100), (3, 3, 100), (3, 4, 100),
            (4, 1, 100), (4, 2, 100), (4, 3, 100), (4, 4, 100);
        """;

    public DatabaseManager() {
        initializeDatabase();
    }

    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute(SCHEMA_SQL); // Create tables
            stmt.execute(DATA_SQL);  // Insert or update data
            System.out.println("Database initialized successfully");
        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    public List<Branch> getBranches() throws SQLException {
        List<Branch> branches = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name FROM branches")) {
            while (rs.next()) {
                branches.add(new Branch(rs.getInt("id"), rs.getString("name")));
            }
            System.out.println("Fetched branches from DB: " + branches);
        } catch (SQLException e) {
            System.err.println("Error fetching branches: " + e.getMessage());
            throw e;
        }
        return branches;
    }

    public List<Drink> getDrinks() throws SQLException {
        List<Drink> drinks = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name FROM drinks")) {
            while (rs.next()) {
                drinks.add(new Drink(rs.getInt("id"), rs.getString("name"), 0.0)); // Placeholder price
            }
            System.out.println("Fetched drinks from DB: " + drinks);
        } catch (SQLException e) {
            System.err.println("Error fetching drinks: " + e.getMessage());
            throw e;
        }
        return drinks;
    }

    public int addCustomer(String name) throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO customers (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Failed to retrieve customer ID");
    }

    public void placeOrder(int customerId, int branchId, int drinkId, int quantity) throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            conn.setAutoCommit(false);
            try (PreparedStatement orderStmt = conn.prepareStatement("INSERT INTO orders (customer_id, branch_id, drink_id, quantity) VALUES (?, ?, ?, ?)")) {
                orderStmt.setInt(1, customerId);
                orderStmt.setInt(2, branchId);
                orderStmt.setInt(3, drinkId);
                orderStmt.setInt(4, quantity);
                orderStmt.executeUpdate();

                // Update stock
                try (PreparedStatement stockStmt = conn.prepareStatement(
                    "UPDATE stock SET quantity = quantity - ? WHERE branch_id = ? AND drink_id = ?")) {
                    stockStmt.setInt(1, quantity);
                    stockStmt.setInt(2, branchId);
                    stockStmt.setInt(3, drinkId);
                    int updatedRows = stockStmt.executeUpdate();
                    if (updatedRows == 0) {
                        throw new SQLException("Insufficient stock or invalid branch/drink combination");
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public List<String> checkStockLevels() throws SQLException {
        List<String> alerts = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT b.name, d.name, s.quantity FROM stock s JOIN branches b ON s.branch_id = b.id JOIN drinks d ON s.drink_id = d.id WHERE s.quantity < 50")) {
            while (rs.next()) {
                alerts.add(rs.getString("name") + " at " + rs.getString("name") + " has " + rs.getInt("quantity") + " units");
            }
        }
        return alerts;
    }

    public void addStock(int branchId, int drinkId, int quantity) throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement("MERGE INTO stock (branch_id, drink_id, quantity) KEY (branch_id, drink_id) VALUES (?, ?, ?)")) {
            stmt.setInt(1, branchId);
            stmt.setInt(2, drinkId);
            stmt.setInt(3, quantity);
            stmt.executeUpdate();
        }
    }

    public List<String> getOrdersReport() throws SQLException {
        List<String> report = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT c.name AS customer_name, b.name AS branch_name, d.name AS drink_name, o.quantity, o.order_date FROM orders o JOIN customers c ON o.customer_id = c.id JOIN branches b ON o.branch_id = b.id JOIN drinks d ON o.drink_id = d.id")) {
            while (rs.next()) {
                report.add(rs.getString("customer_name") + " ordered " + rs.getInt("quantity") + " x " + rs.getString("drink_name") + " at " + rs.getString("branch_name") + " on " + rs.getTimestamp("order_date"));
            }
        }
        return report;
    }

    public List<String> getSalesReport() throws SQLException {
        List<String> report = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT b.name AS branch_name, d.name AS drink_name, COUNT(*) as order_count, SUM(o.quantity * d.price) as total_sales FROM orders o JOIN branches b ON o.branch_id = b.id JOIN drinks d ON o.drink_id = d.id GROUP BY b.name, d.name")) {
            while (rs.next()) {
                report.add(rs.getString("branch_name") + " - " + rs.getString("drink_name") + ": " + rs.getInt("order_count") + " orders, $" + rs.getDouble("total_sales"));
            }
        }
        return report;
    }

    public List<String> getAllOrders() throws SQLException {
        List<String> allOrders = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT c.name AS customer_name, b.name AS branch_name, d.name AS drink_name, o.quantity, o.order_date FROM orders o JOIN customers c ON o.customer_id = c.id JOIN branches b ON o.branch_id = b.id JOIN drinks d ON o.drink_id = d.id")) {
            while (rs.next()) {
                allOrders.add(rs.getString("customer_name") + " ordered " + rs.getInt("quantity") + " x " + rs.getString("drink_name") + " at " + rs.getString("branch_name") + " on " + rs.getTimestamp("order_date"));
            }
        }
        return allOrders;
    }
}