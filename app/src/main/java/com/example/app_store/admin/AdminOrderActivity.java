package com.example.app_store.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_store.R;
import com.example.app_store.adapters.AdminOrderAdapter;
import com.example.app_store.models.Order;
import com.example.app_store.models.OrderListResponse;
import com.example.app_store.network.APIService;
import com.example.app_store.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminOrderActivity extends AppCompatActivity {

    private RecyclerView recyclerOrders;
    private EditText etSearch;
    private Spinner spinnerStatus;
    private AdminOrderAdapter adapter;
    private List<Order> orderList = new ArrayList<>();
    private List<Order> filteredList = new ArrayList<>();
    private ArrayAdapter<String> statusAdapter;

    private final String[] statusOptions = {"Tất cả", "Chờ xử lý", "Đang giao", "Đã giao", "Đã hủy", "Hoàn tiền"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_order);
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        recyclerOrders = findViewById(R.id.recycler_orders);
        etSearch = findViewById(R.id.et_search);
        spinnerStatus = findViewById(R.id.spinner_filter_status);

        // Spinner trạng thái
        statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statusOptions);
        spinnerStatus.setAdapter(statusAdapter);

        // RecyclerView
        adapter = new AdminOrderAdapter(this, filteredList);
        recyclerOrders.setLayoutManager(new LinearLayoutManager(this));
        recyclerOrders.setAdapter(adapter);

        fetchOrders();

        // Lọc trạng thái
        spinnerStatus.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                filterOrders();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Tìm kiếm
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterOrders();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchOrders() {
        APIService apiService = RetrofitClient.getInstance().create(APIService.class);
        apiService.getAllOrders().enqueue(new Callback<OrderListResponse>() {
            @Override
            public void onResponse(Call<OrderListResponse> call, Response<OrderListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    orderList = response.body().getOrders();
                    filterOrders();
                } else {
                    Toast.makeText(AdminOrderActivity.this, "Lỗi tải đơn hàng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<OrderListResponse> call, Throwable t) {
                Toast.makeText(AdminOrderActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterOrders() {
        String keyword = etSearch.getText().toString().toLowerCase();
        String selectedStatus = spinnerStatus.getSelectedItem().toString();

        filteredList.clear();

        for (Order order : orderList) {
            boolean matchesSearch = order.getUser_name().toLowerCase().contains(keyword) ||
                    String.valueOf(order.getId()).contains(keyword);
            boolean matchesStatus = selectedStatus.equals("Tất cả") || order.getStatus().equalsIgnoreCase(selectedStatus);

            if (matchesSearch && matchesStatus) {
                filteredList.add(order);
            }
        }

        adapter.notifyDataSetChanged();
    }

    // ✅ Nhận kết quả khi cập nhật trạng thái
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            Order updatedOrder = (Order) data.getSerializableExtra("updated_order");
            if (updatedOrder != null) {
                for (int i = 0; i < orderList.size(); i++) {
                    if (orderList.get(i).getId() == updatedOrder.getId()) {
                        orderList.set(i, updatedOrder);
                        break;
                    }
                }
                filterOrders();
            }
        }
    }
}
