package com.example.app_store.models;

import java.util.List;

public class StatusStatResponse {
    private boolean success;
    private List<StatusCount> data;

    public boolean isSuccess() {
        return success;
    }

    public List<StatusCount> getData() {
        return data;
    }

    public static class StatusCount {
        private String status;
        private int count;

        public String getStatus() {
            return status;
        }

        public int getCount() {
            return count;
        }
    }
}
