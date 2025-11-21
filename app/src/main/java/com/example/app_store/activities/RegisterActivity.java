package com.example.app_store.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputType;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.example.app_store.R;
import com.example.app_store.models.GeneralResponse;
import com.example.app_store.models.RegisterRequest;
import com.example.app_store.network.APIService;
import com.example.app_store.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    EditText edtName, edtEmail, edtPassword, edtPhone, edtAddress;
    Button btnRegister;
    TextView tvGoLogin;
    ImageView ivTogglePassword, ivOpenMap;
    boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Ánh xạ View
        edtName = findViewById(R.id.edt_name);
        edtEmail = findViewById(R.id.edt_old_password);
        edtPassword = findViewById(R.id.edt_password);
        edtPhone = findViewById(R.id.edt_phone);
        edtAddress = findViewById(R.id.edt_address);
        btnRegister = findViewById(R.id.btn_register);
        tvGoLogin = findViewById(R.id.tv_go_login);
        ivTogglePassword = findViewById(R.id.iv_toggle_password);
        ivOpenMap = findViewById(R.id.iv_open_map);

        // 👁 Ẩn/hiện mật khẩu
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

        // 📍 Mở dialog nhập địa chỉ thủ công
        ivOpenMap.setOnClickListener(v -> showManualAddressDialog(edtAddress));

        // ✅ Đăng ký tài khoản
        btnRegister.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();
            String phone = edtPhone.getText().toString().trim();
            String address = edtAddress.getText().toString().trim();

            if (!validateInput(name, email, password, phone, address)) return;

            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận đăng ký")
                    .setMessage("Bạn có chắc chắn muốn tạo tài khoản mới với thông tin này không?")
                    .setPositiveButton("Đồng ý", (dialog, which) ->
                            registerUser(name, email, password, phone, address))
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        // 🔙 Quay lại đăng nhập
        tvGoLogin.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Quay lại đăng nhập")
                    .setMessage("Bạn có muốn quay lại màn hình đăng nhập không? Dữ liệu bạn đã nhập sẽ bị mất.")
                    .setPositiveButton("Có", (dialog, which) -> {
                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        finish();
                    })
                    .setNegativeButton("Không", null)
                    .show();
        });
    }

    // ✅ Kiểm tra dữ liệu form chính
    private boolean validateInput(String name, String email, String password, String phone, String address) {
        if (name.isEmpty()) {
            edtName.setError("Vui lòng nhập họ tên");
            edtName.requestFocus();
            vibrate();
            return false;
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Email không hợp lệ");
            edtEmail.requestFocus();
            vibrate();
            return false;
        }

        if (password.length() < 6) {
            edtPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            edtPassword.requestFocus();
            vibrate();
            return false;
        }

        if (phone.isEmpty() || !phone.matches("^[0-9]{9,11}$")) {
            edtPhone.setError("Số điện thoại không hợp lệ (9–11 số)");
            edtPhone.requestFocus();
            vibrate();
            return false;
        }

        if (address.isEmpty()) {
            edtAddress.setError("Vui lòng nhập địa chỉ");
            edtAddress.requestFocus();
            vibrate();
            return false;
        }

        return true;
    }

    // ✅ Gửi yêu cầu đăng ký
    private void registerUser(String name, String email, String password, String phone, String address) {
        APIService apiService = RetrofitClient.getAPIService();
        RegisterRequest request = new RegisterRequest(name, email, password, phone, address);

        apiService.registerUser(request).enqueue(new Callback<GeneralResponse>() {
            @Override
            public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    GeneralResponse res = response.body();
                    Toast.makeText(RegisterActivity.this, res.getMessage(), Toast.LENGTH_LONG).show();

                    if (res.isSuccess()) {
                        new AlertDialog.Builder(RegisterActivity.this)
                                .setTitle("Đăng ký thành công")
                                .setMessage("Tài khoản của bạn đã được tạo.\nBạn có muốn chuyển đến trang đăng nhập không?")
                                .setPositiveButton("Đồng ý", (dialog, which) -> {
                                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                    finish();
                                })
                                .setNegativeButton("Ở lại", null)
                                .show();
                    }
                } else {
                    Toast.makeText(RegisterActivity.this, "Đăng ký thất bại. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GeneralResponse> call, Throwable t) {
                Toast.makeText(RegisterActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 📍 Dialog nhập địa chỉ thủ công + bắt lỗi + rung nhẹ
    private void showManualAddressDialog(EditText edtAddress) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_address_input, null);

        EditText edtHouse = dialogView.findViewById(R.id.edt_house);
        EditText edtStreet = dialogView.findViewById(R.id.edt_street);
        EditText edtWard = dialogView.findViewById(R.id.edt_ward);
        EditText edtDistrict = dialogView.findViewById(R.id.edt_district);
        EditText edtCity = dialogView.findViewById(R.id.edt_city);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        // ✅ Nút xác nhận
        dialogView.findViewById(R.id.btn_confirm).setOnClickListener(v -> {
            String house = edtHouse.getText().toString().trim();
            String street = edtStreet.getText().toString().trim();
            String ward = edtWard.getText().toString().trim();
            String district = edtDistrict.getText().toString().trim();
            String city = edtCity.getText().toString().trim();

            boolean isValid = true;

            // Kiểm tra từng ô
            if (house.isEmpty()) {
                edtHouse.setError("Vui lòng nhập số nhà");
                isValid = false; vibrate();
            }
            if (street.isEmpty()) {
                edtStreet.setError("Vui lòng nhập tên đường");
                isValid = false; vibrate();
            }
            if (ward.isEmpty()) {
                edtWard.setError("Vui lòng nhập phường / xã");
                isValid = false; vibrate();
            }
            if (district.isEmpty()) {
                edtDistrict.setError("Vui lòng nhập quận / huyện");
                isValid = false; vibrate();
            }
            if (city.isEmpty()) {
                edtCity.setError("Vui lòng nhập tỉnh / thành phố");
                isValid = false; vibrate();
            }

            // Nếu có lỗi → dừng
            if (!isValid) return;

            // Regex kiểm tra hợp lệ
            String fullText = house + street + ward + district + city;
            if (!fullText.matches("^[\\p{L}0-9\\s,./-]+$")) {
                Toast.makeText(this, "Địa chỉ chứa ký tự không hợp lệ", Toast.LENGTH_SHORT).show();
                vibrate();
                return;
            }

            // Gộp địa chỉ hoàn chỉnh
            String fullAddress = house + ", " + street + ", " + ward + ", " + district + ", " + city;
            edtAddress.setText(fullAddress);
            dialog.dismiss();
        });

        // ❌ Nút hủy
        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // 💡 Hàm rung nhẹ khi nhập sai (UX chuyên nghiệp)
    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(100);
        }
    }
}
