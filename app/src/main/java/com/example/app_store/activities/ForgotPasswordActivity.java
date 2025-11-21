package com.example.app_store.activities;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.app_store.R;
import com.example.app_store.models.GeneralResponse;
import com.example.app_store.models.ForgotPasswordRequest;
import com.example.app_store.network.APIService;
import com.example.app_store.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    EditText edtEmail;
    Button btnSend;
    TextView tvBackToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        edtEmail     = findViewById(R.id.edt_old_password);
        btnSend      = findViewById(R.id.btn_update_password);
        tvBackToLogin = findViewById(R.id.tv_back_account);

        // Gửi yêu cầu reset mật khẩu
        btnSend.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();

            if (email.isEmpty()) {
                edtEmail.setError("Vui lòng nhập email của bạn");
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                edtEmail.setError("Email không hợp lệ");
                return;
            }

            sendResetRequest(email);
        });

        tvBackToLogin.setOnClickListener(v -> finish());
    }


    private void sendResetRequest(String email) {
        APIService apiService = RetrofitClient.getAPIService();
        ForgotPasswordRequest request = new ForgotPasswordRequest(email);

        btnSend.setEnabled(false);
        btnSend.setText("Đang xử lý...");

        apiService.forgotPassword(request).enqueue(new Callback<GeneralResponse>() {
            @Override
            public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> response) {

                btnSend.setEnabled(true);
                btnSend.setText("Gửi yêu cầu");

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(ForgotPasswordActivity.this,
                            "Lỗi hệ thống. Vui lòng thử lại.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                GeneralResponse res = response.body();

                if (res.isSuccess()) {
                    // 💚 Mật khẩu reset thành công
                    Toast.makeText(ForgotPasswordActivity.this,
                            "Mật khẩu đã được đặt lại về: 123456",
                            Toast.LENGTH_LONG).show();
                } else {
                    // ❌ Email không tồn tại / lỗi server
                    Toast.makeText(ForgotPasswordActivity.this,
                            res.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GeneralResponse> call, Throwable t) {
                btnSend.setEnabled(true);
                btnSend.setText("Gửi yêu cầu");

                Toast.makeText(ForgotPasswordActivity.this,
                        "Lỗi kết nối server!",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
