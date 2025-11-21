package com.example.app_store.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ProductResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private List<Product> products;

    public boolean isSuccess() {
        return success;
    }

    public List<Product> getProducts() {
        return products;
    }
}
