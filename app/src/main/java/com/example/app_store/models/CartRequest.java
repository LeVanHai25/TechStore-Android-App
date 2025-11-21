package com.example.app_store.models;

public class CartRequest {
    private int user_id;
    private int product_id;
    private int quantity;

    private double price;

    public CartRequest(int user_id, int product_id, int quantity , double price ) {
        this.user_id = user_id;
        this.product_id = product_id;
        this.quantity = quantity;
        this.price = price;
    }
    public CartRequest(int user_id, int product_id, int quantity ) {
        this.user_id = user_id;
        this.product_id = product_id;
        this.quantity = quantity;
    }

    // Getters and Setters
    public int getUser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }

    public int getProduct_id() { return product_id; }
    public void setProduct_id(int product_id) { this.product_id = product_id; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
