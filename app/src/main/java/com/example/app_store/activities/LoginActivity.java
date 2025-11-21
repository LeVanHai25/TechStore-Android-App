package com.example.app_store.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.app_store.R;
import com.example.app_store.admin.AdminDashboardActivity;
import com.example.app_store.models.Admin;
import com.example.app_store.models.LoginRequest;
import com.example.app_store.models.LoginResponse;
import com.example.app_store.models.User;
import com.example.app_store.network.APIService;
import com.example.app_store.network.RetrofitClient;
import com.example.app_store.utils.AdminPrefManager;
import com.example.app_store.utils.SharedPrefManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    EditText edtUsername, edtPassword;
    Button btnLogin;
    TextView tvRegister, tvForgotPassword;
    ImageView ivTogglePassword;
    boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // ❗ LUÔN xóa session cũ trước khi login
        SharedPrefManager.getInstance(this).logout();
        AdminPrefManager.getInstance(this).logout();

        // Ánh xạ view
        edtUsername = findViewById(R.id.edt_username);
        edtPassword = findViewById(R.id.edt_password);
        ivTogglePassword = findViewById(R.id.iv_toggle_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.tv_register);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);

        // 👁 Ẩn / hiện mật khẩu
        ivTogglePassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivTogglePassword.setImageResource(R.drawable.ic_eye_off);
            } else {
                edtPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivTogglePassword.setImageResource(R.drawable.ic_eye);
            }
            edtPassword.setSelection(edtPassword.getText().length());
            isPasswordVisible = !isPasswordVisible;
        });

        // 🟢 Đăng nhập
        btnLogin.setOnClickListener(v -> {
            String email = edtUsername.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (!validateInput(email, password)) return;
            loginUser(email, password);
        });

        // 🔹 Chuyển sang Đăng ký
        tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));

        // 🔹 Chuyển sang Quên mật khẩu
        tvForgotPassword.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    // ======================== VALIDATE INPUT ========================
    private boolean validateInput(String email, String password) {
        if (email.isEmpty()) {
            edtUsername.setError("Vui lòng nhập email");
            edtUsername.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtUsername.setError("Email không hợp lệ");
            edtUsername.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            edtPassword.setError("Vui lòng nhập mật khẩu");
            edtPassword.requestFocus();
            return false;
        }

        if (password.length() < 3) {
            edtPassword.setError("Mật khẩu phải có ít nhất 3 ký tự");
            edtPassword.requestFocus();
            return false;
        }

        return true;
    }

    // ======================== GỬI YÊU CẦU LOGIN ========================
    private void loginUser(String email, String password) {
        APIService apiService = RetrofitClient.getAPIService();
        LoginRequest request = new LoginRequest(email, password);

        btnLogin.setEnabled(false);
        btnLogin.setText("Đang kiểm tra...");

        apiService.loginUser(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Đăng nhập");

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(LoginActivity.this, "Không thể đăng nhập, vui lòng thử lại!", Toast.LENGTH_SHORT).show();
                    return;
                }

                LoginResponse res = response.body();

                if (!res.isSuccess()) {
                    // ❌ SERVER TỪ CHỐI
                    Toast.makeText(LoginActivity.this, res.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                }

                // 🟢 ADMIN LOGIN
                if ("admin".equals(res.getRole())) {
                    Admin admin = res.getAdmin();
                    AdminPrefManager.getInstance(LoginActivity.this).saveAdmin(admin);
                    startActivity(new Intent(LoginActivity.this, AdminDashboardActivity.class));
                    finish();
                    return;
                }

                // 🟢 USER LOGIN
                User user = res.getUser();

                // ❗ KIỂM TRA STATUS USER
                if (user != null && "blocked".equals(user.getStatus())) {
                    Toast.makeText(LoginActivity.this,
                            "Tài khoản của bạn đã bị khóa, không thể đăng nhập!",
                            Toast.LENGTH_LONG).show();
                    return; // ❗ KHÔNG cho login
                }

                // ✔ User hợp lệ → lưu session
                SharedPrefManager.getInstance(LoginActivity.this).saveUser(user);
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Đăng nhập");
                Toast.makeText(LoginActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
