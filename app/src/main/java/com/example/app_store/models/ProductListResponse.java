package com.example.app_store.models;

import java.util.List;

public class ProductListResponse {
    private boolean success;
    private List<Product> data;
    private int total;
    private int page;
    private int limit;

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public List<Product> getData() {
        return data;
    }

    public int getTotal() {
        return total;
    }

    public int getPage() {
        return page;
    }

    public int getLimit() {
        return limit;
    }
}
