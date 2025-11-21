package com.example.app_store.models;

import java.io.Serializable;

public class CartItem implements Serializable {
    private int id;
    private int quantity;
    private Product product;
    private double price; // giá theo phiên bản (trắng/xám)

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public CartItem(int id, int quantity, Product product) {
        this.id = id;
        this.quantity = quantity;
        this.product = product;
    }

    public int getId() { return id; }
    public int getQuantity() { return quantity; }
    public Product getProduct() { return product; }

    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setProduct(Product product) { this.product = product; }
}
