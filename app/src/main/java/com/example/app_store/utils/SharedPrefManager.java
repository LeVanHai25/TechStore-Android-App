package com.example.app_store.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.app_store.models.User;

public class SharedPrefManager {

    private static final String SHARED_PREF_NAME = "app_store_prefs";

    // Các khóa dùng để lưu thông tin người dùng
    private static final String KEY_ID = "key_id";
    private static final String KEY_NAME = "key_name";
    private static final String KEY_EMAIL = "key_email";
    private static final String KEY_ROLE = "key_role";
    private static final String KEY_PHONE = "key_phone";
    private static final String KEY_ADDRESS = "key_address";
    private static final String KEY_PASSWORD = "key_password";

    private static SharedPrefManager mInstance;
    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    // Constructor private để áp dụng Singleton
    private SharedPrefManager(Context context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

// Lấy instance (Singleton)

    public static synchronized SharedPrefManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new SharedPrefManager(context);
        }
        return mInstance;
    }

    // Lưu thông tin người dùng hoặc admin
    public void saveUser(User user) {
        editor.putInt(KEY_ID, user.getId());
        editor.putString(KEY_NAME, user.getName());
        editor.putString(KEY_EMAIL, user.getEmail());
        editor.putString(KEY_ROLE, user.getRole());
        editor.putString(KEY_PHONE, user.getPhone());
        editor.putString(KEY_ADDRESS, user.getAddress());
        editor.putString(KEY_PASSWORD, user.getPassword());
        editor.apply();
    }

    // Lấy thông tin người dùng hiện tại
    public User getUser() {
        User user = new User(
                sharedPreferences.getInt(KEY_ID, -1),
                sharedPreferences.getString(KEY_NAME, null),
                sharedPreferences.getString(KEY_EMAIL, null),
                sharedPreferences.getString(KEY_ROLE, null)
        );
        user.setPhone(sharedPreferences.getString(KEY_PHONE, null));
        user.setAddress(sharedPreferences.getString(KEY_ADDRESS, null));
        user.setPassword(sharedPreferences.getString(KEY_PASSWORD, null));
        return user;
    }

    // Kiểm tra đã đăng nhập hay chưa
    public boolean isLoggedIn() {
        return sharedPreferences.getInt(KEY_ID, -1) != -1;
    }

    // Xoá toàn bộ dữ liệu đăng nhập (đăng xuất)
    public void logout() {
        editor.clear();
        editor.apply();
    }
}
