package com.example.app_store.models;

public class UploadResponse {
    private boolean success;
    private String filename;
    private String message;

    public boolean isSuccess() { return success; }
    public String getFilename() { return filename; }
    public String getMessage() { return message; }
}
