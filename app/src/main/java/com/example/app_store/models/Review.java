package com.example.app_store.models;

public class Review {
    private int id;
    private int rating;
    private String comment;
    private String created_at;
    private String user_name;
    private String admin_reply;

    // Constructor không tham số cho Gson
    public Review() {
    }

    public Review(int id, int rating, String comment, String created_at, String user_name, String admin_reply) {
        this.id = id;
        this.rating = rating;
        this.comment = comment;
        this.created_at = created_at;
        this.user_name = user_name;
        this.admin_reply = admin_reply;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public String getAdmin_reply() {
        return admin_reply;
    }

    public void setAdmin_reply(String admin_reply) {
        this.admin_reply = admin_reply;
    }
}
