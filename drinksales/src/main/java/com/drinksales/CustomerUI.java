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
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class CustomerUI extends Application {
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

        Label nameLabel = new Label("Customer Name:");
        TextField nameField = new TextField();
        Label branchLabel = new Label("Branch:");
        ComboBox<Branch> branchCombo = new ComboBox<>();
        Label drinkLabel = new Label("Drink:");
        ComboBox<Drink> drinkCombo = new ComboBox<>();
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

        orderButton.setOnAction(e -> {
            if (nameField.getText().isEmpty() || branchCombo.getValue() == null || drinkCombo.getValue() == null) {
                statusLabel.setText("Please fill all fields.");
                return;
            }
            try {
                out.writeObject("PLACE_ORDER");
                out.writeObject(nameField.getText());
                out.writeObject(branchCombo.getValue().getId());
                out.writeObject(drinkCombo.getValue().getId());
                out.flush();
                String response = (String) in.readObject();
                statusLabel.setText(response);
                Object alertsObj = in.readObject();
                if (alertsObj instanceof List) {
                    List<String> stockAlerts = (List<String>) alertsObj;
                    if (!stockAlerts.isEmpty()) {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Stock Alert");
                        alert.setContentText(String.join("\n", stockAlerts));
                        alert.showAndWait();
                    }
                }
            } catch (IOException | ClassNotFoundException ex) {
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