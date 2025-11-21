package com.example.app_store.models;

import java.util.List;

public class RevenueResponse {
    private boolean success;
    private List<RevenueData> data;

    public boolean isSuccess() {
        return success;
    }

    public List<RevenueData> getData() {
        return data;
    }

    public static class RevenueData {
        private String label; // Ví dụ: "Tháng 7", "Ngày 27", ...
        private float total;

        public String getLabel() {
            return label;
        }

        public float getTotal() {
            return total;
        }
    }
}
