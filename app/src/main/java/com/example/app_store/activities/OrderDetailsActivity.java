package com.example.app_store.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_store.R;
import com.example.app_store.adapters.OrderItemAdapter;
import com.example.app_store.models.Order;
import com.example.app_store.models.GeneralResponse;
import com.example.app_store.network.APIService;
import com.example.app_store.network.RetrofitClient;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderDetailsActivity extends AppCompatActivity {

    private TextView tvOrderId, tvStatus, tvTotal, tvDate;
    private RecyclerView recyclerItems;
    private Button btnCancel;
    private APIService apiService;
    private Order order;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        tvOrderId = findViewById(R.id.tv_order_id);
        tvStatus = findViewById(R.id.tv_order_status);
        tvTotal = findViewById(R.id.tv_order_total);
        tvDate = findViewById(R.id.tv_order_date);
        btnCancel = findViewById(R.id.btn_cancel_order);
        recyclerItems = findViewById(R.id.recycler_order_items);
        recyclerItems.setLayoutManager(new LinearLayoutManager(this));

        apiService = RetrofitClient.getAPIService();

        order = (Order) getIntent().getSerializableExtra("order");
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        if (order != null) {
            tvOrderId.setText("Mã đơn: #" + order.getOrder_id());
            tvStatus.setText("Trạng thái: " + order.getStatus());
            tvDate.setText("Ngày: " + order.getCreated_at());
            tvTotal.setText("Tổng: " + String.format("%,.0f₫", order.getTotal()));

            recyclerItems.setAdapter(new OrderItemAdapter(this, order.getItems()));

            if (!order.getStatus().equals("Chờ xử lý")) {
                btnCancel.setEnabled(false);
            }

            btnCancel.setOnClickListener(v -> showCancelDialog());
        }
    }

    private void showCancelDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận")
                .setMessage("Bạn có chắc muốn hủy đơn hàng này?")
                .setPositiveButton("Có", (dialog, which) -> cancelOrder())
                .setNegativeButton("Không", null)
                .show();
    }

    private void cancelOrder() {
        Map<String, Integer> data = new HashMap<>();
        data.put("order_id", order.getOrder_id());

        apiService.cancelOrder(data).enqueue(new Callback<GeneralResponse>() {
            @Override
            public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(OrderDetailsActivity.this, "Đã hủy đơn hàng", Toast.LENGTH_SHORT).show();

// ẨN NÚT HỦY NGAY LẬP TỨC
                    btnCancel.setVisibility(View.GONE);

// Cập nhật trạng thái UI
                    tvStatus.setText("Trạng thái: Đã hủy");
                    tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                } else {
                    Toast.makeText(OrderDetailsActivity.this, "Không thể hủy đơn", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GeneralResponse> call, Throwable t) {
                Toast.makeText(OrderDetailsActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
