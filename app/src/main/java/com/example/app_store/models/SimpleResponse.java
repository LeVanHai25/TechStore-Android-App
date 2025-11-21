package com.example.app_store.models;

import java.io.Serializable;

public class SimpleResponse implements Serializable {
    private boolean success;
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
