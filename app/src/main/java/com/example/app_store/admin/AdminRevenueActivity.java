package com.example.app_store.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.app_store.R;
import com.example.app_store.models.RevenueResponse;
import com.example.app_store.models.StatusStatResponse;
import com.example.app_store.network.APIService;
import com.example.app_store.network.RetrofitClient;

import java.text.DecimalFormat;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminRevenueActivity extends AppCompatActivity {

    ImageButton btnBack;
    Spinner spinnerDay, spinnerMonth, spinnerYear;
    Button btnFilter;

    TextView tvTotalRevenue, tvDelivered, tvCancelled, tvShipping, tvReturned, tvPending;

    DecimalFormat moneyFormat = new DecimalFormat("#,### đ");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_revenue);

        initViews();
        setupListeners();

        // Load mặc định
        int day = parseSpinner(spinnerDay);
        int month = parseSpinner(spinnerMonth);
        int year = parseSpinner(spinnerYear);

        loadRevenue(day, month, year);
        loadOrderStats(day, month, year);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);

        spinnerDay = findViewById(R.id.spinner_day);
        spinnerMonth = findViewById(R.id.spinner_month);
        spinnerYear = findViewById(R.id.spinner_year);
        btnFilter = findViewById(R.id.btn_filter);

        tvTotalRevenue = findViewById(R.id.tv_total_revenue);
        tvDelivered = findViewById(R.id.tv_delivered);
        tvCancelled = findViewById(R.id.tv_cancelled);
        tvShipping = findViewById(R.id.tv_shipping);
        tvReturned = findViewById(R.id.tv_returned);
        tvPending = findViewById(R.id.tv_pending);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnFilter.setOnClickListener(v -> {
            int day = parseSpinner(spinnerDay);
            int month = parseSpinner(spinnerMonth);
            int year = parseSpinner(spinnerYear);

            loadRevenue(day, month, year);
            loadOrderStats(day, month, year);
        });
    }

    private int parseSpinner(Spinner spinner) {
        try {
            return Integer.parseInt(spinner.getSelectedItem().toString());
        } catch (Exception e) {
            return 0;
        }
    }

    // ============================ //
    //     API DOANH THU           //
    // ============================ //
    private void loadRevenue(int day, int month, int year) {
        APIService api = RetrofitClient.getAPIService();

        api.getRevenueFiltered(day, month, year).enqueue(new Callback<RevenueResponse>() {
            @Override
            public void onResponse(Call<RevenueResponse> call, Response<RevenueResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(AdminRevenueActivity.this, "Lỗi tải doanh thu", Toast.LENGTH_SHORT).show();
                    return;
                }

                RevenueResponse res = response.body();

                if (res.isSuccess() && res.getData() != null) {

                    double total = 0;
                    for (RevenueResponse.RevenueData item : res.getData()) {
                        total += item.getTotal();
                    }

                    tvTotalRevenue.setText(moneyFormat.format(total));
                } else {
                    tvTotalRevenue.setText("0 đ");
                }
            }

            @Override
            public void onFailure(Call<RevenueResponse> call, Throwable t) {
                Toast.makeText(AdminRevenueActivity.this, "Lỗi kết nối doanh thu", Toast.LENGTH_SHORT).show();
            }
        });
    }


    // ============================ //
    //     API TRẠNG THÁI ĐƠN      //
    // ============================ //
    private void loadOrderStats(int day, int month, int year) {
        APIService api = RetrofitClient.getAPIService();

        api.getStatusStatsFiltered(day, month, year).enqueue(new Callback<StatusStatResponse>() {
            @Override
            public void onResponse(Call<StatusStatResponse> call, Response<StatusStatResponse> res) {
                if (!res.isSuccessful() || res.body() == null) {
                    Toast.makeText(AdminRevenueActivity.this, "Lỗi tải thống kê đơn", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (res.body().isSuccess()) {
                    List<StatusStatResponse.StatusCount> list = res.body().getData();

                    // gán mặc định 0
                    int delivered = 0, cancelled = 0, shipping = 0, returned = 0, pending = 0;

                    for (StatusStatResponse.StatusCount item : list) {
                        switch (item.getStatus()) {
                            case "Đã giao":
                                delivered = item.getCount();
                                break;
                            case "Đã hủy":
                                cancelled = item.getCount();
                                break;
                            case "Đang giao":
                                shipping = item.getCount();
                                break;
                            case "Hoàn hàng":
                                returned = item.getCount();
                                break;
                            case "Chờ xử lý":
                                pending = item.getCount();
                                break;
                        }
                    }

                    // Đổ lên UI
                    tvDelivered.setText(String.valueOf(delivered));
                    tvCancelled.setText(String.valueOf(cancelled));
                    tvShipping.setText(String.valueOf(shipping));
                    tvReturned.setText(String.valueOf(returned));
                    tvPending.setText(String.valueOf(pending));

                }
            }

            @Override
            public void onFailure(Call<StatusStatResponse> call, Throwable t) {
                Toast.makeText(AdminRevenueActivity.this, "Lỗi kết nối thống kê đơn", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
