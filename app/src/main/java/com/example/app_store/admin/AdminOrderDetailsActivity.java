package com.example.app_store.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_store.R;
import com.example.app_store.adapters.OrderItemAdapter;
import com.example.app_store.models.Order;
import com.example.app_store.models.OrderItem;
import com.example.app_store.models.SimpleResponse;
import com.example.app_store.network.APIClient;
import com.example.app_store.network.APIService;

import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminOrderDetailsActivity extends AppCompatActivity {

    private TextView tvOrderId, tvUser, tvEmail, tvPhone, tvAddress, tvDate, tvTotal, tvStatus;
    private Spinner spinnerStatus;
    private RecyclerView rvOrderItems;
    private Button btnUpdate;
    private Order order;
    private APIService apiService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_order_details);
        apiService = APIClient.getClient().create(APIService.class);
        order = (Order) getIntent().getSerializableExtra("order");
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Ánh xạ view
        tvOrderId = findViewById(R.id.tv_order_id);
        tvUser = findViewById(R.id.tv_user);
        tvEmail = findViewById(R.id.tv_email);
        tvPhone = findViewById(R.id.tv_phone);
        tvAddress = findViewById(R.id.tv_address);
        tvDate = findViewById(R.id.tv_date);
        tvTotal = findViewById(R.id.tv_total);
        tvStatus = findViewById(R.id.tv_status);
        spinnerStatus = findViewById(R.id.spinner_status);
        rvOrderItems = findViewById(R.id.rv_order_items);
        btnUpdate = findViewById(R.id.btn_update_status);
        rvOrderItems.setLayoutManager(new LinearLayoutManager(this));

        if (order != null) {
            displayOrderDetails();
        }
    }

    private void displayOrderDetails() {
        tvOrderId.setText("Mã đơn: #" + order.getId());
        tvUser.setText("Người đặt: " + order.getUser_name());
        tvEmail.setText("Email: " + order.getUser_email());
        tvPhone.setText("SĐT: " + order.getPhone());
        tvAddress.setText("Địa chỉ: " + order.getAddress());
        tvDate.setText("Ngày đặt: " + order.getCreated_at());
        tvTotal.setText(String.format("Tổng tiền: %,.0f₫", order.getTotal()));
        tvStatus.setText("Trạng thái hiện tại: " + order.getStatus());

        // Gán danh sách sản phẩm trong đơn
        OrderItemAdapter adapter = new OrderItemAdapter(this, order.getItems());
        rvOrderItems.setAdapter(adapter);

        // Trạng thái có thể cập nhật
        List<String> availableStatuses = Arrays.asList("Chờ xử lý", "Đang giao", "Đã giao", "Đã hủy","Hoàn hàng");
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, availableStatuses);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(spinnerAdapter);

        spinnerStatus.setSelection(availableStatuses.indexOf(order.getStatus()));

        // Nếu đơn hàng đã xử lý, ẩn nút cập nhật
        List<String> editableStatuses = Arrays.asList("Chờ xử lý", "Đang giao", "Đã giao");
        if (!editableStatuses.contains(order.getStatus())) {
            spinnerStatus.setEnabled(false);
            btnUpdate.setVisibility(View.GONE);
        } else {
            btnUpdate.setOnClickListener(v -> updateStatus());
        }

    }

    private void updateStatus() {
        String selectedStatus = spinnerStatus.getSelectedItem().toString();
        if (selectedStatus.equals(order.getStatus())) {
            Toast.makeText(this, "Trạng thái không thay đổi", Toast.LENGTH_SHORT).show();
            return;
        }

        // Gọi API cập nhật
        apiService.updateOrderStatus(order.getOrder_id(), selectedStatus)
                .enqueue(new Callback<SimpleResponse>() {
                    @Override
                    public void onResponse(Call<SimpleResponse> call, Response<SimpleResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Toast.makeText(AdminOrderDetailsActivity.this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                            Intent resultIntent = new Intent();
                            order.setStatus(selectedStatus); // cập nhật trạng thái mới
                            resultIntent.putExtra("updated_order", order);
                            setResult(RESULT_OK, resultIntent);
                            finish(); // Quay về danh sách đơn hàng
                        } else {
                            Toast.makeText(AdminOrderDetailsActivity.this, "Thất bại: " + response.body().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<SimpleResponse> call, Throwable t) {
                        Toast.makeText(AdminOrderDetailsActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

    }

}
