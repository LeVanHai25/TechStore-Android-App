package com.example.app_store.models;

import java.util.List;

public class ReviewListResponse {
    private boolean success;
    private String message;
    private Data data;

    public boolean isSuccess() { return success; }
    public Data getData() { return data; }

    public static class Data {
        private List<Review> reviews;
        private Summary summary;

        public List<Review> getReviews() { return reviews; }
        public Summary getSummary() { return summary; }
    }

    public static class Summary {
        private float avg_rating;
        private int total;

        public float getAvg_rating() { return avg_rating; }
        public int getTotal() { return total; }
    }
}
