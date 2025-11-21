package com.example.app_store.activities;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.app_store.R;
import com.example.app_store.models.GeneralResponse;
import com.example.app_store.network.APIService;
import com.example.app_store.network.RetrofitClient;
import com.example.app_store.utils.SharedPrefManager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText edtOld, edtNew, edtConfirm;
    private ImageView ivOldToggle, ivNewToggle, ivConfirmToggle;
    private Button btnUpdate;
    private TextView btnBackAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // ánh xạ các view
        edtOld = findViewById(R.id.edt_old_password);
        edtNew = findViewById(R.id.edt_new_password);
        edtConfirm = findViewById(R.id.edt_confirm_password);

        ivOldToggle = findViewById(R.id.iv_old_toggle);
        ivNewToggle = findViewById(R.id.iv_new_toggle);
        ivConfirmToggle = findViewById(R.id.iv_confirm_toggle);

        btnUpdate = findViewById(R.id.btn_update_password);
        btnBackAccount = findViewById(R.id.tv_back_account);

        // xử lý ẩn/hiện mật khẩu
        setupToggle(edtOld, ivOldToggle);
        setupToggle(edtNew, ivNewToggle);
        setupToggle(edtConfirm, ivConfirmToggle);

        // quay lại trang tài khoản
        btnBackAccount.setOnClickListener(v -> finish());

        btnUpdate.setOnClickListener(v -> showConfirmDialog());

    }
    private void showConfirmDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xác nhận đổi mật khẩu")
                .setMessage("Bạn có chắc chắn muốn đổi mật khẩu không?")
                .setIcon(R.drawable.ic_lock)
                .setPositiveButton("Đổi mật khẩu", (dialog, which) -> changePassword())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void setupToggle(EditText edt, ImageView icon) {
        icon.setOnClickListener(v -> {
            if (edt.getInputType() ==
                    (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {

                edt.setInputType(InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

                icon.setImageResource(R.drawable.ic_eye);
            } else {
                edt.setInputType(InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD);

                icon.setImageResource(R.drawable.ic_eye_off);
            }
            edt.setSelection(edt.getText().length()); // giữ vị trí con trỏ
        });
    }

    private void changePassword() {
        String oldPass = edtOld.getText().toString().trim();
        String newPass = edtNew.getText().toString().trim();
        String confirm = edtConfirm.getText().toString().trim();

        if (TextUtils.isEmpty(oldPass)) {
            edtOld.setError("Nhập mật khẩu cũ");
            return;
        }
        if (TextUtils.isEmpty(newPass)) {
            edtNew.setError("Nhập mật khẩu mới");
            return;
        }
        if (newPass.equals(oldPass)) {
            edtNew.setError("Mật khẩu mới không được giống mật khẩu cũ");
            return;
        }
        if (!newPass.equals(confirm)) {
            edtConfirm.setError("Mật khẩu nhập lại không khớp");
            return;
        }

        int userId = SharedPrefManager.getInstance(this).getUser().getId();

        Map<String, String> body = new HashMap<>();
        body.put("user_id", String.valueOf(userId));
        body.put("old_password", oldPass);
        body.put("new_password", newPass);

        APIService api = RetrofitClient.getAPIService();
        api.changePassword(body).enqueue(new Callback<GeneralResponse>() {
            @Override
            public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> response) {
                if (response.isSuccessful() && response.body() != null) {

                    // —— THÔNG BÁO ĐỔI MẬT KHẨU THÀNH CÔNG ——
                    new androidx.appcompat.app.AlertDialog.Builder(ChangePasswordActivity.this)
                            .setTitle("Thành công")
                            .setMessage("Mật khẩu đã được đổi thành công!")
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .setCancelable(false)
                            .setPositiveButton("OK", (d, w) -> finish())
                            .show();
                }
            }

            @Override
            public void onFailure(Call<GeneralResponse> call, Throwable t) {
                Toast.makeText(ChangePasswordActivity.this,
                        "Lỗi kết nối! Vui lòng thử lại.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
