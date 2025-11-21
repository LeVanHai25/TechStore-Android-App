package com.example.app_store.models;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AdminReviewResponse {
    private boolean success;
    private Data data;

    public boolean isSuccess() { return success; }
    public Data getData() { return data; }

    public static class Data {

        @SerializedName("reviews_admin")
        private List<ReviewAdmin> reviewsAdmin;

        public List<ReviewAdmin> getReviews() {
            return reviewsAdmin;
        }
    }
}
