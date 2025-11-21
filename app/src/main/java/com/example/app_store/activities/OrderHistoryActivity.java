package com.example.app_store.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_store.R;
import com.example.app_store.adapters.OrderHistoryAdapter;
import com.example.app_store.models.Order;
import com.example.app_store.models.OrderResponse;
import com.example.app_store.models.User;
import com.example.app_store.network.APIService;
import com.example.app_store.network.RetrofitClient;
import com.example.app_store.utils.SharedPrefManager;
import com.example.app_store.utils.OrderCache;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private OrderHistoryAdapter adapter;
    private APIService apiService;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        // Ánh xạ view
        recyclerView = findViewById(R.id.recycler_order_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        apiService = RetrofitClient.getAPIService();
        currentUser = SharedPrefManager.getInstance(this).getUser();

        // Nút quay lại
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Load danh sách đơn hàng
        loadOrders();

        // Setup bottom nav
        setupBottomNavigation();
    }

    // -------------------- Load danh sách đơn hàng --------------------
    private void loadOrders() {
        apiService.getOrders(currentUser.getId()).enqueue(new Callback<OrderResponse>() {
            @Override
            public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Order> orders = response.body().getOrders();
                    
                    // Lưu orders vào cache để có thể truy cập sau
                    OrderCache.setOrders(orders);

                    adapter = new OrderHistoryAdapter(OrderHistoryActivity.this, orders, order -> {
                        // Chuyển đến màn hình chi tiết đơn hàng
                        Intent intent = new Intent(OrderHistoryActivity.this, OrderDetailsActivity.class);
                        // Truyền cả order object và order_id để đảm bảo
                        intent.putExtra("order", order); // Order implements Serializable
                        int orderId = (order.getId() != 0) ? order.getId() : order.getOrder_id();
                        intent.putExtra("order_id", orderId);
                        startActivity(intent);
                    });

                    recyclerView.setAdapter(adapter);
                } else {
                    Toast.makeText(OrderHistoryActivity.this, "Không thể tải đơn hàng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<OrderResponse> call, Throwable t) {
                Toast.makeText(OrderHistoryActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // -------------------- Bottom Navigation --------------------
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_order);  // Đánh dấu tab Order đang được chọn

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(OrderHistoryActivity.this, MainActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
                return true;
            }

            else if (id == R.id.nav_cart) {
                startActivity(new Intent(OrderHistoryActivity.this, CartActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
                return true;
            }

            else if (id == R.id.nav_order) {
                return true; // Đang ở trang Order → không làm gì
            }

            else if (id == R.id.nav_account) {
                startActivity(new Intent(OrderHistoryActivity.this, AccountActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
                return true;
            }

            return false;
        });
    }
}
