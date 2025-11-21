package com.example.app_store.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.app_store.R;
import com.example.app_store.adapters.ReviewAdapter;
import com.example.app_store.models.AddReviewRequest;
import com.example.app_store.models.CartRequest;
import com.example.app_store.models.GeneralResponse;
import com.example.app_store.models.Product;
import com.example.app_store.models.Review;
import com.example.app_store.models.ReviewListResponse;
import com.example.app_store.network.APIService;
import com.example.app_store.network.RetrofitClient;
import com.example.app_store.utils.SharedPrefManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailActivity extends AppCompatActivity {

    private ImageView imgProduct;
    private TextView tvName, tvPrice, tvDescription;

    private RatingBar rbMyRating, ratingAvgBar;
    private TextView tvAvgRating;
    private EditText edtReviewComment;
    private Button btnSendReview;

    private RecyclerView rvReviews;
    private ReviewAdapter adapter;
    private List<Review> reviewList = new ArrayList<>();

    private Button btnAddToCart;
    private Product product;
    private APIService apiService;

    private int productId;

    private int selectedVersion = 0; // 0: thường | 1: Trắng | 2: Xám
    private double basePrice;

    private final String BASE_IMAGE_URL = "http://10.0.2.2/backend/images/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        apiService = RetrofitClient.getAPIService();

        // ÁNH XẠ VIEW
        imgProduct = findViewById(R.id.img_product_detail);
        tvName = findViewById(R.id.tv_product_name);
        tvPrice = findViewById(R.id.tv_product_price);
        tvDescription = findViewById(R.id.tv_product_description);
        btnAddToCart = findViewById(R.id.btn_add_to_cart);

        rbMyRating = findViewById(R.id.rb_my_rating);
        ratingAvgBar = findViewById(R.id.rating_avg_bar);
        tvAvgRating = findViewById(R.id.tv_avg_rating);
        edtReviewComment = findViewById(R.id.edt_review_comment);
        btnSendReview = findViewById(R.id.btn_send_review);
        rvReviews = findViewById(R.id.rv_reviews);

        ImageButton btnBack = findViewById(R.id.btn_back);
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReviewAdapter(this, reviewList);
        rvReviews.setAdapter(adapter);

        // NÚT TRỞ VỀ HOME
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(ProductDetailActivity.this, MainActivity.class));
            finish();
        });

        // NHẬN DỮ LIỆU SẢN PHẨM
        product = (Product) getIntent().getSerializableExtra("product");

        if (product != null) {
            productId = product.getId();
            basePrice = product.getPrice();

            tvName.setText(product.getName());
            tvPrice.setText(String.format("%,.0f₫", basePrice));
            tvDescription.setText(product.getDescription());

            Glide.with(this)
                    .load(BASE_IMAGE_URL + product.getImage())
                    .into(imgProduct);

            // Nhấn vào ảnh → phóng to
            imgProduct.setOnClickListener(v -> {
                Intent i = new Intent(ProductDetailActivity.this, ViewImageActivity.class);
                i.putExtra("image", BASE_IMAGE_URL + product.getImage());
                startActivity(i);
            });

            // Nút chọn phiên bản
            Button btnWhite = findViewById(R.id.btn_white);
            Button btnGray = findViewById(R.id.btn_gray);

            // ► Chọn màu Trắng
            btnWhite.setOnClickListener(v -> {
                selectedVersion = 1;

                double newPrice = basePrice + 1_000_000;
                tvPrice.setText(String.format("%,.0f₫", newPrice));

                btnWhite.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFD54F")));
                btnGray.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            });

            // ► Chọn màu Xám
            btnGray.setOnClickListener(v -> {
                selectedVersion = 2;

                double newPrice = basePrice + 500_000;
                tvPrice.setText(String.format("%,.0f₫", newPrice));

                btnGray.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFD54F")));
                btnWhite.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            });

            btnAddToCart.setOnClickListener(v -> addToCart());

            loadReviews();
        }

        btnSendReview.setOnClickListener(v -> sendReview());
    }

    // ======================= LOAD REVIEWS =======================
    private void loadReviews() {
        apiService.getReviews(productId).enqueue(new Callback<ReviewListResponse>() {
            @Override
            public void onResponse(Call<ReviewListResponse> call, Response<ReviewListResponse> res) {
                if (!res.isSuccessful() || res.body() == null) return;

                ReviewListResponse.Data data = res.body().getData();

                float avg = data.getSummary().getAvg_rating();
                int total = data.getSummary().getTotal();

                tvAvgRating.setText(String.format("%.1f/5 (%d đánh giá)", avg, total));
                ratingAvgBar.setRating(avg);

                reviewList.clear();
                reviewList.addAll(data.getReviews());
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Call<ReviewListResponse> call, Throwable t) {}
        });
    }

    // ======================= GỬI REVIEW =======================
    private void sendReview() {
        int rating = (int) rbMyRating.getRating();
        String comment = edtReviewComment.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Vui lòng chọn số sao!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (comment.length() < 2) {
            Toast.makeText(this, "Nhận xét phải có ít nhất 2 ký tự!", Toast.LENGTH_SHORT).show();
            return;
        }
        int userId = SharedPrefManager.getInstance(this).getUser().getId();
        AddReviewRequest request = new AddReviewRequest(productId, userId, rating, comment);

        apiService.addReview(request).enqueue(new Callback<GeneralResponse>() {
            @Override
            public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> response) {
                if (response.isSuccessful() && response.body().isSuccess()) {
                    edtReviewComment.setText("");
                    rbMyRating.setRating(0);
                    loadReviews();
                }
            }

            @Override
            public void onFailure(Call<GeneralResponse> call, Throwable t) {}
        });
    }

    // ======================= THÊM GIỎ HÀNG =======================
    private void addToCart() {
        if (SharedPrefManager.getInstance(this).getUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        double finalPrice = basePrice;

        if (selectedVersion == 1) finalPrice += 1_000_000; // Trắng
        if (selectedVersion == 2) finalPrice += 500_000;   // Xám

        int userId = SharedPrefManager.getInstance(this).getUser().getId();

        CartRequest request = new CartRequest(userId, productId, 1, finalPrice);

        apiService.addToCart(request).enqueue(new Callback<GeneralResponse>() {
            @Override
            public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> response) {
                Toast.makeText(ProductDetailActivity.this, "Đã thêm vào giỏ hàng!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<GeneralResponse> call, Throwable t) {}
        });
    }
}
