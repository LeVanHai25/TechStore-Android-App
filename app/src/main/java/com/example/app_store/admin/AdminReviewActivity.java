package com.example.app_store.admin;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_store.R;
import com.example.app_store.adapters.AdminReviewAdapter;
import android.widget.Toast;

import com.example.app_store.models.AdminReviewResponse;
import com.example.app_store.models.GeneralResponse;
import com.example.app_store.models.ReviewAdmin;
import com.example.app_store.models.ReviewListResponse;
import com.example.app_store.network.APIService;
import com.example.app_store.network.RetrofitClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminReviewActivity extends AppCompatActivity {

    RecyclerView rv;
    AdminReviewAdapter adapter;
    List<ReviewAdmin> list = new ArrayList<>();
    APIService api;

    int productId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_review_list);

        // Lấy product_id từ Intent (hoặc để -1 để lấy tất cả)
        productId = getIntent().getIntExtra("product_id", -1);

        api = RetrofitClient.getAPIService();
        rv = findViewById(R.id.rvAdminReviews);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AdminReviewAdapter(this, list,
                new AdminReviewAdapter.ReviewListener() {
                    @Override
                    public void onDelete(int id) {
                        deleteReview(id);
                    }

                    @Override
                    public void onReply(int id, String msg) {
                        replyReview(id, msg);
                    }
                });

        rv.setAdapter(adapter);

        loadData();
    }

    private void loadData() {
        api.getAllAdminReviews().enqueue(new Callback<AdminReviewResponse>() {
            @Override
            public void onResponse(Call<AdminReviewResponse> call, Response<AdminReviewResponse> res) {
                if (res.isSuccessful() && res.body() != null && res.body().isSuccess()) {

                    if (res.body().getData() != null && res.body().getData().getReviews() != null) {
                        list.clear();
                        list.addAll(res.body().getData().getReviews());
                        adapter.notifyDataSetChanged();
                    }
                }
            }
            @Override
            public void onFailure(Call<AdminReviewResponse> call, Throwable t) {}
        });
    }


    private void deleteReview(int id) {
        api.deleteReview(id).enqueue(new Callback<GeneralResponse>() {
            @Override
            public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> res) {
                loadData();
            }
            @Override
            public void onFailure(Call<GeneralResponse> call, Throwable t) {}
        });
    }

    private void replyReview(int id, String msg) {
        if (msg == null || msg.trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập phản hồi", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (msg.trim().length() < 2) {
            Toast.makeText(this, "Phản hồi phải có ít nhất 2 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }
        
        api.replyReview(id, msg).enqueue(new Callback<GeneralResponse>() {
            @Override
            public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> res) {
                if (res.isSuccessful() && res.body() != null) {
                    if (res.body().isSuccess()) {
                        Toast.makeText(AdminReviewActivity.this, "Phản hồi thành công!", Toast.LENGTH_SHORT).show();
                        loadData(); // Refresh lại danh sách
                    } else {
                        Toast.makeText(AdminReviewActivity.this, "Lỗi: " + (res.body().getMessage() != null ? res.body().getMessage() : "Không thể cập nhật"), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(AdminReviewActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<GeneralResponse> call, Throwable t) {
                Toast.makeText(AdminReviewActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}
