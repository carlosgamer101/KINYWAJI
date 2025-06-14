package com.drinksales;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class CustomerUI extends Application {
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

        Label nameLabel = new Label("Customer Name:");
        TextField nameField = new TextField();
        Label branchLabel = new Label("Branch:");
        ComboBox<String> branchCombo = new ComboBox<>();
        Label drinkLabel = new Label("Drink:");
        ComboBox<String> drinkCombo = new ComboBox<>();
        Label quantityLabel = new Label("Quantity:");
        TextField quantityField = new TextField();
        Button orderButton = new Button("Place Order");
        Label statusLabel = new Label();

        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(branchLabel, 0, 1);
        grid.add(branchCombo, 1, 1);
        grid.add(drinkLabel, 0, 2);
        grid.add(drinkCombo, 1, 2);
        grid.add(quantityLabel, 0, 3);
        grid.add(quantityField, 1, 3);
        grid.add(orderButton, 1, 4);
        grid.add(statusLabel, 1, 5);

        try {
            System.out.println("Sending GET_BRANCHES");
            out.println("GET_BRANCHES");
            String branchResponse = in.readLine();
            System.out.println("Received branches: " + branchResponse);
            if (branchResponse != null && !branchResponse.trim().isEmpty() && branchResponse.startsWith("[")) {
                branchResponse = branchResponse.substring(1, branchResponse.length() - 1);
                String[] branches = branchResponse.split("\",\"");
                for (String branch : branches) {
                    branchCombo.getItems().add(branch.replace("\"", "").trim());
                }
            } else {
                System.out.println("No valid branch data received");
            }

            System.out.println("Sending GET_DRINKS");
            out.println("GET_DRINKS");
            String drinkResponse = in.readLine();
            System.out.println("Received drinks: " + drinkResponse);
            if (drinkResponse != null && !drinkResponse.trim().isEmpty() && drinkResponse.startsWith("[")) {
                drinkResponse = drinkResponse.substring(1, drinkResponse.length() - 1);
                String[] drinks = drinkResponse.split("\",\"");
                for (String drink : drinks) {
                    drinkCombo.getItems().add(drink.replace("\"", "").trim());
                }
            } else {
                System.out.println("No valid drink data received");
            }
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error fetching branches or drinks: " + e.getMessage());
        }

        orderButton.setOnAction(e -> {
            System.out.println("Order button clicked");
            if (nameField.getText().isEmpty() || branchCombo.getValue() == null || drinkCombo.getValue() == null || quantityField.getText().isEmpty()) {
                statusLabel.setText("Please fill all fields.");
                System.out.println("Validation failed: Missing fields");
                return;
            }
            try {
                int quantity = Integer.parseInt(quantityField.getText());
                if (quantity <= 0) {
                    statusLabel.setText("Quantity must be greater than 0.");
                    System.out.println("Validation failed: Invalid quantity");
                    return;
                }
                System.out.println("Sending PLACE_ORDER command");
                out.println("PLACE_ORDER");
                out.println(nameField.getText());
                out.println(branchCombo.getItems().indexOf(branchCombo.getValue()) + 1); // 1-based index
                out.println(drinkCombo.getItems().indexOf(drinkCombo.getValue()) + 1);  // 1-based index
                out.println(quantity); // Send quantity
                System.out.println("Waiting for server response");
                String response = in.readLine();
                System.out.println("Received server response: " + response);
                statusLabel.setText(response);
                String alerts = in.readLine();
                System.out.println("Received stock alerts: " + alerts);
                if (alerts != null && !alerts.equals("[]")) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Stock Alert");
                    alert.setContentText(alerts.substring(1, alerts.length() - 1).replace("\"", ""));
                    alert.showAndWait();
                }
            } catch (NumberFormatException ex) {
                statusLabel.setText("Quantity must be a number.");
                System.out.println("Exception: Invalid number format - " + ex.getMessage());
            } catch (IOException ex) {
                statusLabel.setText("Error placing order: " + ex.getMessage());
                System.out.println("Exception: IO error - " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        Scene scene = new Scene(grid, 400, 300);
        primaryStage.setTitle("Customer Order System");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}