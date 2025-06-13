package com.drinksales;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class AdminUI extends Application {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    @Override
    public void start(Stage primaryStage) {
        try {
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            System.out.println("Connected to server at " + SERVER_ADDRESS + ":" + SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to connect to server: " + e.getMessage());
            alert.showAndWait();
            return;
        }

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setVgap(10);
        grid.setHgap(10);

        Button ordersReportButton = new Button("View Orders Report");
        Button salesReportButton = new Button("View Sales Report");
        Label branchLabel = new Label("Branch:");
        ComboBox<Branch> branchCombo = new ComboBox<>();
        Label drinkLabel = new Label("Drink:");
        ComboBox<Drink> drinkCombo = new ComboBox<>();
        Label quantityLabel = new Label("Quantity:");
        TextField quantityField = new TextField();
        Button addStockButton = new Button("Add Stock");
        Button checkStockButton = new Button("Check Stock Levels");
        Label statusLabel = new Label();

        grid.add(ordersReportButton, 0, 0, 2, 1);
        grid.add(salesReportButton, 0, 1, 2, 1);
        grid.add(branchLabel, 0, 2);
        grid.add(branchCombo, 1, 2);
        grid.add(drinkLabel, 0, 3);
        grid.add(drinkCombo, 1, 3);
        grid.add(quantityLabel, 0, 4);
        grid.add(quantityField, 1, 4);
        grid.add(addStockButton, 1, 5);
        grid.add(checkStockButton, 0, 6, 2, 1);
        grid.add(statusLabel, 0, 7, 2, 1);

        try {
            System.out.println("Sending GET_BRANCHES");
            out.writeObject("GET_BRANCHES");
            out.flush();
            Object branchesObj = in.readObject();
            System.out.println("Received branches object: " + branchesObj);
            if (branchesObj instanceof List) {
                List<Branch> branches = (List<Branch>) branchesObj;
                System.out.println("Branches received: " + branches);
                branchCombo.getItems().addAll(branches);
            } else {
                System.out.println("Unexpected branches object type: " + branchesObj.getClass());
                statusLabel.setText("Error: Invalid branches data received");
            }

            System.out.println("Sending GET_DRINKS");
            out.writeObject("GET_DRINKS");
            out.flush();
            Object drinksObj = in.readObject();
            System.out.println("Received drinks object: " + drinksObj);
            if (drinksObj instanceof List) {
                List<Drink> drinks = (List<Drink>) drinksObj;
                System.out.println("Drinks received: " + drinks);
                drinkCombo.getItems().addAll(drinks);
            } else {
                System.out.println("Unexpected drinks object type: " + drinksObj.getClass());
                statusLabel.setText("Error: Invalid drinks data received");
            }
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error fetching branches or drinks (IO): " + (e.getMessage() != null ? e.getMessage() : e.toString()));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            statusLabel.setText("Error fetching branches or drinks (ClassNotFound): " + e.getMessage());
        }

        ordersReportButton.setOnAction(e -> {
            try {
                out.writeObject("GET_ORDERS_REPORT");
                out.flush();
                List<String> report = (List<String>) in.readObject();
                TextArea textArea = new TextArea(String.join("\n", report));
                textArea.setEditable(false);
                Stage reportStage = new Stage();
                reportStage.setScene(new Scene(textArea, 400, 300));
                reportStage.setTitle("Orders Report");
                reportStage.show();
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
                statusLabel.setText("Error fetching orders report: " + ex.getMessage());
            }
        });

        salesReportButton.setOnAction(e -> {
            try {
                out.writeObject("GET_SALES_REPORT");
                out.flush();
                List<String> report = (List<String>) in.readObject();
                TextArea textArea = new TextArea(String.join("\n", report));
                textArea.setEditable(false);
                Stage reportStage = new Stage();
                reportStage.setScene(new Scene(textArea, 400, 300));
                reportStage.setTitle("Sales Report");
                reportStage.show();
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
                statusLabel.setText("Error fetching sales report: " + ex.getMessage());
            }
        });

        addStockButton.setOnAction(e -> {
            try {
                int quantity = Integer.parseInt(quantityField.getText());
                if (branchCombo.getValue() == null || drinkCombo.getValue() == null || quantity <= 0) {
                    statusLabel.setText("Please select branch, drink, and valid quantity.");
                    return;
                }
                out.writeObject("ADD_STOCK");
                out.writeObject(branchCombo.getValue().getId());
                out.writeObject(drinkCombo.getValue().getId());
                out.writeObject(quantity);
                out.flush();
                String response = (String) in.readObject();
                statusLabel.setText(response);
            } catch (NumberFormatException ex) {
                statusLabel.setText("Quantity must be a number.");
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
                statusLabel.setText("Error adding stock: " + ex.getMessage());
            }
        });

        checkStockButton.setOnAction(e -> {
            try {
                out.writeObject("CHECK_STOCK");
                out.flush();
                List<String> alerts = (List<String>) in.readObject();
                if (alerts.isEmpty()) {
                    statusLabel.setText("No stock alerts.");
                } else {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Stock Alerts");
                    alert.setContentText(String.join("\n", alerts));
                    alert.showAndWait();
                }
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
                statusLabel.setText("Error checking stock: " + ex.getMessage());
            }
        });

        Scene scene = new Scene(grid, 400, 400);
        primaryStage.setTitle("Admin Control Panel");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}