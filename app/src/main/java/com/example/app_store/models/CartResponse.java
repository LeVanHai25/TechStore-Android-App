package com.example.app_store.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CartResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName(value = "cart_items", alternate = {"cart"})
    private List<CartItem> cartItems;

    public boolean isSuccess() {
        return success;
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }
}
