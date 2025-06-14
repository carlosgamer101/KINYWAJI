package com.drinksales;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final DatabaseManager db;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket socket, DatabaseManager db) {
        this.clientSocket = socket;
        this.db = db;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true); // Auto-flush
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            System.out.println("Connected to client: " + clientSocket.getInetAddress());

            String command;
            while ((command = in.readLine()) != null) {
                System.out.println("Received raw command: '" + command + "'");
                if (command == null || command.trim().isEmpty()) {
                    System.err.println("Empty or null command received, skipping");
                    continue;
                }
                System.out.println("Processing command: " + command);
                switch (command) {
                    case "GET_BRANCHES":
                        List<Branch> branches = db.getBranches();
                        System.out.println("Fetched branches from DB: " + branches);
                        StringBuilder branchJson = new StringBuilder("[");
                        for (int i = 0; i < branches.size(); i++) {
                            branchJson.append("\"").append(branches.get(i).getName()).append("\"");
                            if (i < branches.size() - 1) branchJson.append(",");
                        }
                        branchJson.append("]");
                        String branchResponse = branchJson.toString();
                        System.out.println("Prepared branch response: " + branchResponse);
                        out.println(branchResponse);
                        System.out.println("Sent branches: " + branchResponse);
                        break;
                    case "GET_DRINKS":
                        List<Drink> drinks = db.getDrinks();
                        System.out.println("Fetched drinks from DB: " + drinks);
                        StringBuilder drinkJson = new StringBuilder("[");
                        for (int i = 0; i < drinks.size(); i++) {
                            drinkJson.append("\"").append(drinks.get(i).getName()).append("\"");
                            if (i < drinks.size() - 1) drinkJson.append(",");
                        }
                        drinkJson.append("]");
                        String drinkResponse = drinkJson.toString();
                        System.out.println("Prepared drink response: " + drinkResponse);
                        out.println(drinkResponse);
                        System.out.println("Sent drinks: " + drinkResponse);
                        break;
                    case "PLACE_ORDER":
                        try {
                            String customerName = in.readLine();
                            System.out.println("Received customer name: " + customerName);
                            int branchId = Integer.parseInt(in.readLine());
                            System.out.println("Received branch ID: " + branchId);
                            int drinkId = Integer.parseInt(in.readLine());
                            System.out.println("Received drink ID: " + drinkId);
                            int quantity = Integer.parseInt(in.readLine());
                            System.out.println("Received quantity: " + quantity);
                            System.out.println("Processing order for " + customerName + ", branch: " + branchId + ", drink: " + drinkId + ", quantity: " + quantity);
                            int customerId = db.addCustomer(customerName);
                            System.out.println("Added customer ID: " + customerId);
                            db.placeOrder(customerId, branchId, drinkId, quantity);
                            System.out.println("Order placed in database");
                            List<String> stockAlerts = db.checkStockLevels();
                            System.out.println("Stock alerts: " + stockAlerts);
                            out.println("Order placed successfully!");
                            out.println(stockAlerts.isEmpty() ? "[]" : "[" + String.join(",", stockAlerts) + "]");
                            System.out.println("Order response sent");
                        } catch (SQLException e) {
                            System.err.println("SQLException in PLACE_ORDER: " + e.getMessage());
                            out.println("Error placing order: " + e.getMessage());
                            e.printStackTrace();
                        } catch (NumberFormatException e) {
                            System.err.println("NumberFormatException in PLACE_ORDER: " + e.getMessage());
                            out.println("Invalid data received");
                        } catch (Exception e) {
                            System.err.println("Unexpected exception in PLACE_ORDER: " + e.getMessage());
                            out.println("Unexpected error: " + e.getMessage());
                            e.printStackTrace();
                        }
                        break;
                    case "GET_ORDERS_REPORT":
                        List<String> ordersReport = db.getOrdersReport();
                        out.println("[" + String.join(",", ordersReport) + "]");
                        System.out.println("Orders report sent");
                        break;
                    case "GET_SALES_REPORT":
                        List<String> salesReport = db.getSalesReport();
                        out.println("[" + String.join(",", salesReport) + "]");
                        System.out.println("Sales report sent");
                        break;
                    case "ADD_STOCK":
                        int stockBranchId = Integer.parseInt(in.readLine());
                        int stockDrinkId = Integer.parseInt(in.readLine());
                        int stockQuantity = Integer.parseInt(in.readLine());
                        System.out.println("Adding stock: branch=" + stockBranchId + ", drink=" + stockDrinkId + ", quantity=" + stockQuantity);
                        db.addStock(stockBranchId, stockDrinkId, stockQuantity);
                        out.println("Stock updated successfully!");
                        System.out.println("Stock update response sent");
                        break;
                    case "CHECK_STOCK":
                        List<String> stockNotifications = db.checkStockLevels();
                        out.println("[" + String.join(",", stockNotifications) + "]");
                        System.out.println("Stock alerts sent");
                        break;
                    case "GET_ALL_ORDERS":
                        List<String> allOrders = db.getAllOrders();
                        out.println("[" + String.join(",", allOrders) + "]");
                        System.out.println("All orders sent: " + allOrders);
                        break;
                    default:
                        System.err.println("Unknown command: " + command);
                        out.println("Invalid command");
                }
                System.out.println("Command processing completed");
            }
        } catch (IOException e) {
            System.err.println("IOException in ClientHandler: " + e.getMessage());
            e.printStackTrace();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                clientSocket.close();
                System.out.println("Closed connection for client: " + clientSocket.getInetAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}