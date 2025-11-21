package com.example.app_store.admin;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.app_store.R;
import com.example.app_store.admin.AdminProductActivity;
import com.example.app_store.activities.LoginActivity;
import com.example.app_store.models.RevenueResponse;
import com.example.app_store.models.StatusStatResponse;
import com.example.app_store.network.APIClient;
import com.example.app_store.network.APIService;
import com.example.app_store.network.RetrofitClient;
import com.example.app_store.utils.AdminPrefManager;
import com.example.app_store.utils.SharedPrefManager;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDashboardActivity extends AppCompatActivity {

    private Button btnManageProducts, btnManageOrders, btnManageUsers;
    ImageButton btnLogout;
    Spinner spinnerDay, spinnerMonth, spinnerYear;
    Button btnFilter;
    BarChart barChart;

    private TextView tvWelcome;
    private PieChart pieChart;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        barChart = findViewById(R.id.bar_chart_revenue);

        pieChart = findViewById(R.id.pie_chart_status);
        spinnerDay = findViewById(R.id.spinner_day);
        spinnerMonth = findViewById(R.id.spinner_month);
        spinnerYear = findViewById(R.id.spinner_year);
        btnFilter = findViewById(R.id.btn_filter);

        LinearLayout cardReviews = findViewById(R.id.card_reviews);
        LinearLayout cardPromotions = findViewById(R.id.card_promotions);

        cardReviews.setOnClickListener(v -> {
            Intent i = new Intent(this, AdminReviewActivity.class);
            startActivity(i);
        });

        cardPromotions.setOnClickListener(v -> {
            Intent i = new Intent(this, AdminPromotionActivity.class);
            startActivity(i);
        });
        btnFilter.setOnClickListener(v -> {
            int day = parseSpinner(spinnerDay);
            int month = parseSpinner(spinnerMonth);
            int year = parseSpinner(spinnerYear);

            loadStatusPieChart(day, month, year);
            loadRevenueChart(day, month, year);
        });
        // Ánh xạ view
        tvWelcome = findViewById(R.id.tv_welcome);
//        btnManageProducts = findViewById(R.id.btn_manage_products);
//        btnManageOrders = findViewById(R.id.btn_manage_orders);
//        btnManageUsers = findViewById(R.id.btn_manage_users);
        btnLogout = findViewById(R.id.btn_logout);

        String adminName = AdminPrefManager.getInstance(this).getAdmin().getName();

        tvWelcome.setText("Chào mừng " + adminName + "!");
        LinearLayout cardUsers = findViewById(R.id.card_users);
        LinearLayout cardProducts = findViewById(R.id.card_products);
        LinearLayout cardOrders = findViewById(R.id.card_orders);
        LinearLayout cardRevenue = findViewById(R.id.card_revenue);

        cardUsers.setOnClickListener(v -> {
            // Mở màn hình quản lý người dùng
            startActivity(new Intent(this, AdminUserActivity.class));
        });

        cardProducts.setOnClickListener(v -> {
            // Mở màn hình quản lý sản phẩm
            startActivity(new Intent(this, AdminProductActivity.class));
        });

        cardOrders.setOnClickListener(v -> {
            // Mở màn hình quản lý đơn hàng
            startActivity(new Intent(this, AdminOrderActivity.class));
        });

        cardRevenue.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminRevenueActivity.class);
            startActivity(intent);
        });



        // Sự kiện các button
//        btnManageProducts.setOnClickListener(v ->
//                startActivity(new Intent(this, AdminProductActivity.class))
//        );

//        btnManageOrders.setOnClickListener(v ->
//                startActivity(new Intent(this, AdminOrderActivity.class)) // bạn sẽ tạo activity này
//        );
//
//        btnManageUsers.setOnClickListener(v ->
//                startActivity(new Intent(this, AdminUserActivity.class)) // bạn sẽ tạo activity này
//        );

        btnLogout.setOnClickListener(v -> showLogoutDialog());

    }

    private int parseSpinner(Spinner spinner) {
        String selected = spinner.getSelectedItem().toString();
        try {
            return Integer.parseInt(selected);
        } catch (NumberFormatException e) {
            return 0; // hoặc -1 nếu bạn muốn phân biệt rõ hơn
        }
    }


    private void loadRevenueChart(int day, int month, int year) {
        APIService apiService = RetrofitClient.getInstance().create(APIService.class);

        Call<RevenueResponse> call = apiService.getRevenueFiltered(day, month, year);
        call.enqueue(new Callback<RevenueResponse>() {
            @Override
            public void onResponse(Call<RevenueResponse> call, Response<RevenueResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<RevenueResponse.RevenueData> revenueList = response.body().getData();

                    // Xoá dữ liệu cũ
                    barChart.clear();

                    List<BarEntry> entries = new ArrayList<>();
                    List<String> labels = new ArrayList<>();

                    for (int i = 0; i < revenueList.size(); i++) {
                        RevenueResponse.RevenueData item = revenueList.get(i);
                        entries.add(new BarEntry(i, (float) item.getTotal()));
                        labels.add(item.getLabel() != null ? item.getLabel() : "");
                    }

                    BarDataSet dataSet = new BarDataSet(entries, "Doanh thu (₫)");
                    dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
                    dataSet.setValueTextColor(Color.BLACK);
                    dataSet.setValueTextSize(12f);

                    BarData data = new BarData(dataSet);
                    data.setBarWidth(0.9f);

                    barChart.setData(data);
                    barChart.setFitBars(true);
                    barChart.getDescription().setEnabled(false);

                    XAxis xAxis = barChart.getXAxis();
                    xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                    xAxis.setGranularity(1f);
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

                    barChart.invalidate(); // refresh
                } else {
                    Toast.makeText(AdminDashboardActivity.this, "Không có dữ liệu biểu đồ doanh thu", Toast.LENGTH_SHORT).show();
                    barChart.clear(); // clear nếu không có dữ liệu
                }
            }

            @Override
            public void onFailure(Call<RevenueResponse> call, Throwable t) {
                Toast.makeText(AdminDashboardActivity.this, "Lỗi kết nối biểu đồ doanh thu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadStatusPieChart(int day, int month, int year) {
        APIService apiService = RetrofitClient.getInstance().create(APIService.class);


        Call<StatusStatResponse> call = apiService.getStatusStatsFiltered(day, month, year);
        call.enqueue(new Callback<StatusStatResponse>() {
            @Override
            public void onResponse(Call<StatusStatResponse> call, Response<StatusStatResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<StatusStatResponse.StatusCount> statusList = response.body().getData();

                    List<PieEntry> entries = new ArrayList<>();
                    for (StatusStatResponse.StatusCount item : statusList) {
                        entries.add(new PieEntry(item.getCount(), item.getStatus()));
                    }

                    Map<String, Integer> colorMap = new HashMap<>();
                    colorMap.put("Hoàn hàng", Color.parseColor("#4CAF50"));
                    colorMap.put("Chờ xử lý", Color.parseColor("#2196F3"));
                    colorMap.put("Đang giao", Color.parseColor("#FFC107"));
                    colorMap.put("Đã giao", Color.parseColor("#E91E63"));
                    colorMap.put("Đã hủy", Color.parseColor("#9C27B0"));

                    List<Integer> colors = new ArrayList<>();
                    for (StatusStatResponse.StatusCount item : statusList) {
                        colors.add(colorMap.get(item.getStatus()));
                    }

                    PieDataSet dataSet = new PieDataSet(entries, "Trạng thái đơn hàng");
                    dataSet.setColors(colors);

                    dataSet.setValueTextSize(14f);
                    dataSet.setValueTextColor(Color.WHITE);

                    PieData data = new PieData(dataSet);
                    pieChart.setData(data);
                    pieChart.getDescription().setEnabled(false);
                    pieChart.setUsePercentValues(true);
                    pieChart.setCenterText("Trạng thái đơn");
                    pieChart.setCenterTextSize(14f);
                    pieChart.invalidate(); // refresh
                } else {
                    Toast.makeText(AdminDashboardActivity.this, "Không có dữ liệu Pie Chart", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<StatusStatResponse> call, Throwable t) {
                Toast.makeText(AdminDashboardActivity.this, "Lỗi kết nối Pie Chart", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void showLogoutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xác nhận đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất khỏi tài khoản?")
                .setCancelable(true)
                .setPositiveButton("Đăng xuất", (dialog, which) -> {

                    // XÓA SESSION ADMIN
                    AdminPrefManager.getInstance(this).logout();

                    Toast.makeText(this, "Đăng xuất thành công", Toast.LENGTH_SHORT).show();

                    // Chuyển về màn hình Login
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }




}
