package com.example.app_store.models;

import java.io.Serializable;

public class OrderItem implements Serializable {
    private int product_id;
    private String product_name;
    private String image;
    private int quantity;
    private double price;

    public int getProduct_id() {
        return product_id;
    }

    public String getProduct_name() {
        return product_name;
    }

    public String getImage() {
        return image;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }
}
