package com.example.app_store.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_store.R;
import com.example.app_store.adapters.PromotionAdapter;
import com.example.app_store.models.Promotion;
import com.example.app_store.models.PromotionListResponse;
import com.example.app_store.models.SimpleResponse;
import com.example.app_store.network.APIService;
import com.example.app_store.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminPromotionActivity extends AppCompatActivity {

    private RecyclerView rvPromotions;
    private PromotionAdapter adapter;
    private List<Promotion> promotionList = new ArrayList<>();
    private APIService apiService;
    private Button btnAddPromotion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_promotion);

        apiService = RetrofitClient.getAPIService();
        rvPromotions = findViewById(R.id.rvPromotions);
        btnAddPromotion = findViewById(R.id.btnAddPromotion);
        ImageButton btnBack = findViewById(R.id.btn_back);

        rvPromotions.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PromotionAdapter(this, promotionList, new PromotionAdapter.PromotionListener() {
            @Override
            public void onEdit(Promotion promotion) {
                showEditDialog(promotion);
            }

            @Override
            public void onDelete(int id) {
                deletePromotion(id);
            }
        });

        rvPromotions.setAdapter(adapter);

        btnAddPromotion.setOnClickListener(v -> showAddDialog());

        btnBack.setOnClickListener(v -> finish());

        loadPromotions();
    }

    private void loadPromotions() {
        apiService.getPromotions().enqueue(new Callback<PromotionListResponse>() {
            @Override
            public void onResponse(Call<PromotionListResponse> call, Response<PromotionListResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    promotionList.clear();
                    if (response.body().getData() != null) {
                        promotionList.addAll(response.body().getData());
                    }
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(AdminPromotionActivity.this, "Không thể tải danh sách khuyến mãi", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PromotionListResponse> call, Throwable t) {
                Toast.makeText(AdminPromotionActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_promotion, null);
        EditText edtMinAmount = view.findViewById(R.id.edt_min_amount);
        EditText edtDiscountPercent = view.findViewById(R.id.edt_discount_percent);
        EditText edtDescription = view.findViewById(R.id.edt_description);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Thêm khuyến mãi mới")
                .setView(view)
                .setPositiveButton("Thêm", null)
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnAdd = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnAdd.setOnClickListener(v -> {
                String minAmountStr = edtMinAmount.getText().toString().trim();
                String discountStr = edtDiscountPercent.getText().toString().trim();
                String description = edtDescription.getText().toString().trim();

                if (minAmountStr.isEmpty() || discountStr.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    long minAmount = Long.parseLong(minAmountStr);
                    int discountPercent = Integer.parseInt(discountStr);

                    if (minAmount <= 0 || discountPercent <= 0 || discountPercent > 100) {
                        Toast.makeText(this, "Giá trị không hợp lệ", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Promotion promotion = new Promotion();
                    promotion.setMin_amount(minAmount);
                    promotion.setDiscount_percent(discountPercent);
                    promotion.setDescription(description.isEmpty() ? "Giảm " + discountPercent + "% cho đơn hàng trên " + String.format("%,d₫", minAmount) : description);
                    promotion.setStatus(1);

                    addPromotion(promotion);
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Vui lòng nhập số hợp lệ", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void showEditDialog(Promotion promotion) {
        View view = getLayoutInflater().inflate(R.layout.dialog_promotion, null);
        EditText edtMinAmount = view.findViewById(R.id.edt_min_amount);
        EditText edtDiscountPercent = view.findViewById(R.id.edt_discount_percent);
        EditText edtDescription = view.findViewById(R.id.edt_description);

        edtMinAmount.setText(String.valueOf(promotion.getMin_amount()));
        edtDiscountPercent.setText(String.valueOf(promotion.getDiscount_percent()));
        edtDescription.setText(promotion.getDescription());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Sửa khuyến mãi")
                .setView(view)
                .setPositiveButton("Cập nhật", null)
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnUpdate = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnUpdate.setOnClickListener(v -> {
                String minAmountStr = edtMinAmount.getText().toString().trim();
                String discountStr = edtDiscountPercent.getText().toString().trim();
                String description = edtDescription.getText().toString().trim();

                if (minAmountStr.isEmpty() || discountStr.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    long minAmount = Long.parseLong(minAmountStr);
                    int discountPercent = Integer.parseInt(discountStr);

                    if (minAmount <= 0 || discountPercent <= 0 || discountPercent > 100) {
                        Toast.makeText(this, "Giá trị không hợp lệ", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    promotion.setMin_amount(minAmount);
                    promotion.setDiscount_percent(discountPercent);
                    promotion.setDescription(description.isEmpty() ? "Giảm " + discountPercent + "% cho đơn hàng trên " + String.format("%,d₫", minAmount) : description);

                    updatePromotion(promotion);
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Vui lòng nhập số hợp lệ", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void addPromotion(Promotion promotion) {
        apiService.addPromotion(promotion).enqueue(new Callback<SimpleResponse>() {
            @Override
            public void onResponse(Call<SimpleResponse> call, Response<SimpleResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(AdminPromotionActivity.this, "Thêm khuyến mãi thành công", Toast.LENGTH_SHORT).show();
                    loadPromotions();
                } else {
                    Toast.makeText(AdminPromotionActivity.this, "Không thể thêm khuyến mãi", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SimpleResponse> call, Throwable t) {
                Toast.makeText(AdminPromotionActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updatePromotion(Promotion promotion) {
        apiService.updatePromotion(promotion).enqueue(new Callback<SimpleResponse>() {
            @Override
            public void onResponse(Call<SimpleResponse> call, Response<SimpleResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(AdminPromotionActivity.this, "Cập nhật khuyến mãi thành công", Toast.LENGTH_SHORT).show();
                    loadPromotions();
                } else {
                    Toast.makeText(AdminPromotionActivity.this, "Không thể cập nhật khuyến mãi", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SimpleResponse> call, Throwable t) {
                Toast.makeText(AdminPromotionActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deletePromotion(int id) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa khuyến mãi này?")
                .setPositiveButton("Xóa", (d, w) -> {
                    apiService.deletePromotion(id).enqueue(new Callback<SimpleResponse>() {
                        @Override
                        public void onResponse(Call<SimpleResponse> call, Response<SimpleResponse> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                Toast.makeText(AdminPromotionActivity.this, "Xóa khuyến mãi thành công", Toast.LENGTH_SHORT).show();
                                loadPromotions();
                            } else {
                                Toast.makeText(AdminPromotionActivity.this, "Không thể xóa khuyến mãi", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<SimpleResponse> call, Throwable t) {
                            Toast.makeText(AdminPromotionActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}


