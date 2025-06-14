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
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

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
        Button orderButton = new Button("Place Order");
        Label statusLabel = new Label();

        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(branchLabel, 0, 1);
        grid.add(branchCombo, 1, 1);
        grid.add(drinkLabel, 0, 2);
        grid.add(drinkCombo, 1, 2);
        grid.add(orderButton, 1, 3);
        grid.add(statusLabel, 1, 4);

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

        orderButton.setOnAction(e -> {
            if (nameField.getText().isEmpty() || branchCombo.getValue() == null || drinkCombo.getValue() == null) {
                statusLabel.setText("Please fill all fields.");
                return;
            }
            try {
                out.println("PLACE_ORDER");
                out.println(nameField.getText());
                out.println(branchCombo.getItems().indexOf(branchCombo.getValue()) + 1); // Assume 1-based index
                out.println(drinkCombo.getItems().indexOf(drinkCombo.getValue()) + 1);  // Assume 1-based index
                String response = in.readLine();
                statusLabel.setText(response);
                String alerts = in.readLine();
                if (alerts != null && !alerts.equals("[]")) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Stock Alert");
                    alert.setContentText(alerts.substring(1, alerts.length() - 1).replace("\"", ""));
                    alert.showAndWait();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                statusLabel.setText("Error placing order: " + ex.getMessage());
            }
        });

        Scene scene = new Scene(grid, 400, 250);
        primaryStage.setTitle("Customer Order System");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}