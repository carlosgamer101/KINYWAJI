package com.drinksales;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

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
    private BufferedReader in;
    private PrintWriter out;

    @Override
    public void start(Stage primaryStage) {
        try {
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            System.out.println("Connected to server at " + SERVER_ADDRESS + ":" + SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
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
        ComboBox<String> branchCombo = new ComboBox<>();
        Label drinkLabel = new Label("Drink:");
        ComboBox<String> drinkCombo = new ComboBox<>();
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
            out.println("GET_BRANCHES");
            String branchResponse = in.readLine();
            System.out.println("Received branches: " + branchResponse);
            if (branchResponse != null && branchResponse.startsWith("[")) {
                branchResponse = branchResponse.substring(1, branchResponse.length() - 1);
                String[] branches = branchResponse.split("\",\"");
                for (String branch : branches) {
                    branchCombo.getItems().add(branch.replace("\"", ""));
                }
            }

            System.out.println("Sending GET_DRINKS");
            out.println("GET_DRINKS");
            String drinkResponse = in.readLine();
            System.out.println("Received drinks: " + drinkResponse);
            if (drinkResponse != null && drinkResponse.startsWith("[")) {
                drinkResponse = drinkResponse.substring(1, drinkResponse.length() - 1);
                String[] drinks = drinkResponse.split("\",\"");
                for (String drink : drinks) {
                    drinkCombo.getItems().add(drink.replace("\"", ""));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error fetching branches or drinks: " + e.getMessage());
        }

        ordersReportButton.setOnAction(e -> {
            try {
                out.println("GET_ORDERS_REPORT");
                String report = in.readLine();
                TextArea textArea = new TextArea(report.substring(1, report.length() - 1).replace("\"", ""));
                textArea.setEditable(false);
                Stage reportStage = new Stage();
                reportStage.setScene(new Scene(textArea, 400, 300));
                reportStage.setTitle("Orders Report");
                reportStage.show();
            } catch (IOException ex) {
                ex.printStackTrace();
                statusLabel.setText("Error fetching orders report: " + ex.getMessage());
            }
        });

        salesReportButton.setOnAction(e -> {
            try {
                out.println("GET_SALES_REPORT");
                String report = in.readLine();
                TextArea textArea = new TextArea(report.substring(1, report.length() - 1).replace("\"", ""));
                textArea.setEditable(false);
                Stage reportStage = new Stage();
                reportStage.setScene(new Scene(textArea, 400, 300));
                reportStage.setTitle("Sales Report");
                reportStage.show();
            } catch (IOException ex) {
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
                out.println("ADD_STOCK");
                out.println(branchCombo.getItems().indexOf(branchCombo.getValue()) + 1);
                out.println(drinkCombo.getItems().indexOf(drinkCombo.getValue()) + 1);
                out.println(quantity);
                statusLabel.setText(in.readLine());
            } catch (NumberFormatException ex) {
                statusLabel.setText("Quantity must be a number.");
            } catch (IOException ex) {
                ex.printStackTrace();
                statusLabel.setText("Error adding stock: " + ex.getMessage());
            }
        });

        checkStockButton.setOnAction(e -> {
            try {
                out.println("CHECK_STOCK");
                String alerts = in.readLine();
                if (alerts.equals("[]")) {
                    statusLabel.setText("No stock alerts.");
                } else {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Stock Alerts");
                    alert.setContentText(alerts.substring(1, alerts.length() - 1).replace("\"", ""));
                    alert.showAndWait();
                }
            } catch (IOException ex) {
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