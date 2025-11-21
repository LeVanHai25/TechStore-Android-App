package com.example.app_store.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.app_store.R;
import com.example.app_store.models.GeneralResponse;
import com.example.app_store.models.User;
import com.example.app_store.network.APIService;
import com.example.app_store.network.RetrofitClient;
import com.example.app_store.utils.SharedPrefManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountActivity extends AppCompatActivity {

    private EditText edtName, edtEmail, edtPhone, edtAddress;
    private Button btnUpdate, btnLogout, btnChangePassword;
    private APIService apiService;
    private User currentUser;
    private ProgressDialog progressDialog;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        // 🔙 Nút quay lại trang main
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(AccountActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // 🔹 Ánh xạ view
        edtName = findViewById(R.id.edt_name);
        edtEmail = findViewById(R.id.edt_email);
        edtPhone = findViewById(R.id.edt_phone);
        edtAddress = findViewById(R.id.edt_address);

        btnUpdate = findViewById(R.id.btn_update);
        btnLogout = findViewById(R.id.btn_logout);
        btnChangePassword = findViewById(R.id.btn_change_password);

        bottomNavigationView = findViewById(R.id.bottom_nav);

        // 🔹 API + Get User
        apiService = RetrofitClient.getAPIService();
        currentUser = SharedPrefManager.getInstance(this).getUser();

        // 🔹 Load user info
        if (currentUser != null) {
            edtName.setText(currentUser.getName());
            edtEmail.setText(currentUser.getEmail());
            edtPhone.setText(currentUser.getPhone());
            edtAddress.setText(currentUser.getAddress());
        }

        edtEmail.setEnabled(false); // Email không cho sửa

        // 🔹 Trang đổi mật khẩu
        btnChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(AccountActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
        });

        // 🔹 Cập nhật thông tin
        btnUpdate.setOnClickListener(v -> {
            if (validateInput()) {
                new AlertDialog.Builder(this)
                        .setTitle("Xác nhận cập nhật")
                        .setMessage("Bạn có chắc muốn cập nhật thông tin cá nhân không?")
                        .setIcon(R.drawable.ic_edit)
                        .setPositiveButton("Xác nhận", (dialog, which) -> updateAccount())
                        .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                        .show();
            }
        });

        // 🔹 Đăng xuất
        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận đăng xuất")
                    .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
                    .setIcon(R.drawable.ic_logout)
                    .setPositiveButton("Đăng xuất", (dialog, which) -> {
                        SharedPrefManager.getInstance(this).logout();
                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        setupBottomNavigation();
    }

    // ------------------------------- ⚙️ BOTTOM NAV -------------------------------
    @Override
    protected void onResume() {
        super.onResume();
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_account);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                switchActivity(MainActivity.class);
                return true;
            }
            else if (id == R.id.nav_cart) {
                switchActivity(CartActivity.class);
                return true;
            }
            else if (id == R.id.nav_order) {
                switchActivity(OrderHistoryActivity.class);
                return true;
            }
            else if (id == R.id.nav_account) {
                return true;
            }
            return false;
        });
    }

    private void switchActivity(Class<?> target) {
        if (target == this.getClass()) return;
        Intent intent = new Intent(this, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    // ------------------------------- 🧩 Validate -------------------------------
    private boolean validateInput() {
        String name = edtName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String address = edtAddress.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            edtName.setError("Vui lòng nhập họ tên");
            return false;
        }

        if (TextUtils.isEmpty(phone)) {
            edtPhone.setError("Vui lòng nhập số điện thoại");
            return false;
        }

        if (!Patterns.PHONE.matcher(phone).matches() || phone.length() < 9) {
            edtPhone.setError("Số điện thoại không hợp lệ");
            return false;
        }

        if (TextUtils.isEmpty(address)) {
            edtAddress.setError("Vui lòng nhập địa chỉ");
            return false;
        }

        return true;
    }

    // ------------------------------- 🔧 UPDATE USER -------------------------------
    private void updateAccount() {
        int id = currentUser.getId();
        String name = edtName.getText().toString().trim();
        String email = currentUser.getEmail();
        String phone = edtPhone.getText().toString().trim();
        String address = edtAddress.getText().toString().trim();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang cập nhật thông tin...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        User updateRequest = new User(id, name, email, currentUser.getRole());
        updateRequest.setPhone(phone);
        updateRequest.setAddress(address);

        apiService.updateUser(updateRequest).enqueue(new Callback<GeneralResponse>() {
            @Override
            public void onResponse(@NonNull Call<GeneralResponse> call, @NonNull Response<GeneralResponse> response) {
                progressDialog.dismiss();

                if (response.isSuccessful() && response.body() != null) {

                    if (response.body().isSuccess()) {
                        Toast.makeText(AccountActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();

                        currentUser.setName(name);
                        currentUser.setPhone(phone);
                        currentUser.setAddress(address);

                        SharedPrefManager.getInstance(AccountActivity.this).saveUser(currentUser);
                    }
                    else Toast.makeText(AccountActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(AccountActivity.this, "Không thể cập nhật. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<GeneralResponse> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(AccountActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}

