package com.drinksales;

/**
 * Represents an order placed by a customer for a drink at a specific branch.
 */
public class Order {
    private int id;
    private int customerId;
    private int branchId;
    private int drinkId;

    public Order(int id, int customerId, int branchId, int drinkId) {
        this.id = id;
        this.customerId = customerId;
        this.branchId = branchId;
        this.drinkId = drinkId;
    }

    public int getId() {
        return id;
    }

    public int getCustomerId() {
        return customerId;
    }

    public int getBranchId() {
        return branchId;
    }

    public int getDrinkId() {
        return drinkId;
    }
}