package com.drinksales;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final DatabaseManager db;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ClientHandler(Socket socket, DatabaseManager db) {
        this.clientSocket = socket;
        this.db = db;
    }

    @Override
    public void run() {
        try {
            // Initialize output stream first
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.flush(); // Send stream header
            System.out.println("Output stream initialized for client: " + clientSocket.getInetAddress());
            in = new ObjectInputStream(clientSocket.getInputStream());
            System.out.println("Input stream initialized for client: " + clientSocket.getInetAddress());

            while (true) {
                Object commandObj = in.readObject();
                if (!(commandObj instanceof String)) {
                    System.err.println("Invalid command type: " + commandObj.getClass());
                    out.writeObject("Invalid command type");
                    out.flush();
                    continue;
                }
                String command = (String) commandObj;
                System.out.println("Received command: " + command);

                switch (command) {
                    case "GET_BRANCHES":
                        List<Branch> branches = db.getBranches();
                        System.out.println("Sending branches: " + branches);
                        out.writeObject(branches);
                        out.flush();
                        System.out.println("Branches sent successfully");
                        break;
                    case "GET_DRINKS":
                        List<Drink> drinks = db.getDrinks();
                        System.out.println("Sending drinks: " + drinks);
                        out.writeObject(drinks);
                        out.flush();
                        System.out.println("Drinks sent successfully");
                        break;
                    case "PLACE_ORDER":
                        String customerName = (String) in.readObject();
                        int branchId = (Integer) in.readObject();
                        int drinkId = (Integer) in.readObject();
                        System.out.println("Processing order for " + customerName + ", branch: " + branchId + ", drink: " + drinkId);
                        int customerId = db.addCustomer(customerName);
                        db.placeOrder(customerId, branchId, drinkId);
                        List<String> stockAlerts = db.checkStockLevels(); // Used in PLACE_ORDER
                        out.writeObject("Order placed successfully!");
                        out.writeObject(stockAlerts.isEmpty() ? null : stockAlerts);
                        out.flush();
                        System.out.println("Order response sent");
                        break;
                    case "GET_ORDERS_REPORT":
                        List<String> ordersReport = db.getOrdersReport();
                        out.writeObject(ordersReport);
                        out.flush();
                        System.out.println("Orders report sent");
                        break;
                    case "GET_SALES_REPORT":
                        List<String> salesReport = db.getSalesReport();
                        out.writeObject(salesReport);
                        out.flush();
                        System.out.println("Sales report sent");
                        break;
                    case "ADD_STOCK":
                        int stockBranchId = (Integer) in.readObject();
                        int stockDrinkId = (Integer) in.readObject();
                        int quantity = (Integer) in.readObject();
                        System.out.println("Adding stock: branch=" + stockBranchId + ", drink=" + stockDrinkId + ", quantity=" + quantity);
                        db.addStock(stockBranchId, stockDrinkId, quantity);
                        out.writeObject("Stock updated successfully!");
                        out.flush();
                        System.out.println("Stock update response sent");
                        break;
                    case "CHECK_STOCK":
                        List<String> stockNotifications = db.checkStockLevels(); // Renamed to avoid duplicate
                        out.writeObject(stockNotifications);
                        out.flush();
                        System.out.println("Stock alerts sent");
                        break;
                    default:
                        System.err.println("Unknown command: " + command);
                        out.writeObject("Invalid command");
                        out.flush();
                }
            }
        } catch (EOFException e) {
            System.err.println("EOFException in ClientHandler: client likely disconnected");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IOException in ClientHandler: " + e.getMessage());
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println("ClassNotFoundException in ClientHandler: " + e.getMessage());
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("SQLException in ClientHandler: " + e.getMessage());
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