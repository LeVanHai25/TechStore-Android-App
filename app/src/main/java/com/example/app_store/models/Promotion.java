package com.example.app_store.models;

import java.io.Serializable;

public class Promotion implements Serializable {
    private int id;
    private long min_amount;
    private int discount_percent;
    private String description;
    private int status;
    private String created_at;

    public Promotion() {
    }

    public Promotion(int id, long min_amount, int discount_percent, String description, int status, String created_at) {
        this.id = id;
        this.min_amount = min_amount;
        this.discount_percent = discount_percent;
        this.description = description;
        this.status = status;
        this.created_at = created_at;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getMin_amount() {
        return min_amount;
    }

    public void setMin_amount(long min_amount) {
        this.min_amount = min_amount;
    }

    public int getDiscount_percent() {
        return discount_percent;
    }

    public void setDiscount_percent(int discount_percent) {
        this.discount_percent = discount_percent;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }
}


