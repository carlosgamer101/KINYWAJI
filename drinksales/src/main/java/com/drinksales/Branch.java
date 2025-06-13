package com.drinksales;

import java.io.Serializable;

public class Branch implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private String name;

    public Branch(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}