package com.example.app_store.models;

public class AddReviewRequest {
    private int product_id;
    private int user_id;
    private int rating;
    private String comment;

    public AddReviewRequest(int product_id, int user_id, int rating, String comment) {
        this.product_id = product_id;
        this.user_id = user_id;
        this.rating = rating;
        this.comment = comment;
    }
}
