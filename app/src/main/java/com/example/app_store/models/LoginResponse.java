package com.example.app_store.models;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {

    private boolean success;
    private String message;
    private String role;

    private User user;
    private Admin admin;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getRole() {
        return role;
    }

    public User getUser() {
        return user;
    }

    public Admin getAdmin() {
        return admin;
    }
}
