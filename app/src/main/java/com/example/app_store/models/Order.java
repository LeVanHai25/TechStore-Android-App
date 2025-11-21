package com.example.app_store.models;

import java.io.Serializable;
import java.util.List;

public class Order implements Serializable {
    private int order_id;
    private int id;
    private int user_id;
    private String user_name;
    private String user_email;
    private String phone;
    private String address;
    private String note;
    private double total;
    private String status;
    private String created_at;
    private List<OrderItem> items;

    // Getters
    public int getOrder_id() {
        return order_id;
    }

    public int getId() {
        return id;
    }

    public int getUser_id() {
        return user_id;
    }

    public String getUser_name() {
        return user_name;
    }

    public String getUser_email() {
        return user_email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getNote() {
        return note;
    }

    public double getTotal() {
        return total;
    }

    public String getStatus() {
        return status;
    }

    public String getCreated_at() {
        return created_at;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setOrder_id(int order_id) {
        this.order_id = order_id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public void setUser_email(String user_email) {
        this.user_email = user_email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
    // Optionally: Setters (nếu cần cập nhật dữ liệu trong ứng dụng)
}
