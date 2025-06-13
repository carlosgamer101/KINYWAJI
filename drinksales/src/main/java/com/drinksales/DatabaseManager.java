package com.drinksales;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String URL = "jdbc:h2:~/KINYWAJI";
    private static final String USER = "sa";
    private static final String PASSWORD = "";
    private static final int STOCK_THRESHOLD = 50;

    public DatabaseManager() {
        initializeDatabase();
    }

    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            System.out.println("Running schema.sql from classpath");
            stmt.execute("RUNSCRIPT FROM 'classpath:schema.sql'");
            System.out.println("Database initialized successfully");
        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int addCustomer(String name) throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO customers (name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new SQLException("Failed to retrieve customer ID");
        }
    }

    public void placeOrder(int customerId, int branchId, int drinkId) throws SQLException {
        Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
        conn.setAutoCommit(false);
        try {
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE stock SET quantity = quantity - 1 WHERE branch_id = ? AND drink_id = ? AND quantity > 0");
            stmt.setInt(1, branchId);
            stmt.setInt(2, drinkId);
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Order failed: insufficient stock or invalid branch/drink.");
            }

            stmt = conn.prepareStatement(
                "INSERT INTO orders (customer_id, branch_id, drink_id) VALUES (?, ?, ?)");
            stmt.setInt(1, customerId);
            stmt.setInt(2, branchId);
            stmt.setInt(3, drinkId);
            stmt.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
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
        }
        return branches;
    }

    public List<Drink> getDrinks() throws SQLException {
        List<Drink> drinks = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, price FROM drinks")) {
            while (rs.next()) {
                drinks.add(new Drink(rs.getInt("id"), rs.getString("name"), rs.getDouble("price")));
            }
        }
        return drinks;
    }

    public List<String> getOrdersReport() throws SQLException {
        List<String> report = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT c.name AS customer, b.name AS branch, d.name AS drink " +
                 "FROM orders o " +
                 "JOIN customers c ON o.customer_id = c.id " +
                 "JOIN branches b ON o.branch_id = b.id " +
                 "JOIN drinks d ON o.drink_id = d.id")) {
            while (rs.next()) {
                report.add(String.format("Customer: %s, Branch: %s, Drink: %s",
                    rs.getString("customer"), rs.getString("branch"), rs.getString("drink")));
            }
        }
        return report;
    }

    public List<String> getSalesReport() throws SQLException {
        List<String> report = new ArrayList<>();
        double totalSales = 0;
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT b.name AS branch, COUNT(o.id) AS order_count, SUM(d.price) AS sales " +
                 "FROM orders o " +
                 "JOIN branches b ON o.branch_id = b.id " +
                 "JOIN drinks d ON o.drink_id = d.id " +
                 "GROUP BY b.name")) {
            while (rs.next()) {
                double sales = rs.getDouble("sales");
                report.add(String.format("Branch: %s, Total Sales: KSh %.2f", 
                    rs.getString("branch"), sales));
                totalSales += sales;
            }
            report.add(String.format("Overall Total Sales: KSh %.2f", totalSales));
        }
        return report;
    }

    public void addStock(int branchId, int drinkId, int quantity) throws SQLException {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(
                 "MERGE INTO stock (branch_id, drink_id, quantity) KEY(branch_id, drink_id) " +
                 "VALUES (?, ?, COALESCE((SELECT quantity FROM stock WHERE branch_id = ? AND drink_id = ?), 0) + ?)")) {
            stmt.setInt(1, branchId);
            stmt.setInt(2, drinkId);
            stmt.setInt(3, branchId);
            stmt.setInt(4, drinkId);
            stmt.setInt(5, quantity);
            stmt.executeUpdate();
        }
    }

    public List<String> checkStockLevels() throws SQLException {
        List<String> alerts = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT b.name AS branch, SUM(s.quantity) AS total_stock " +
                 "FROM stock s " +
                 "JOIN branches b ON s.branch_id = b.id " +
                 "GROUP BY b.name " +
                 "HAVING SUM(s.quantity) < " + STOCK_THRESHOLD)) {
            while (rs.next()) {
                alerts.add(String.format("Alert: %s branch stock below threshold (Total: %d)", 
                    rs.getString("branch"), rs.getInt("total_stock")));
            }
        }
        return alerts;
    }
}