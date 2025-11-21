package com.example.app_store.models;

public class OrderRequest {
    private int user_id;
    private String address;
    private String phone;
    private String note;
    private double total; // QUAN TRỌNG: Tổng tiền sau khi áp dụng khuyến mãi

    public OrderRequest(int user_id, String address, String phone, String note, double total) {
        this.user_id = user_id;
        this.address = address;
        this.phone = phone;
        this.note = note;
        this.total = total;
    }

    // Getters and Setters
    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }
}
