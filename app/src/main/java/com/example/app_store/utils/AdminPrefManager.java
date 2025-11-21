package com.example.app_store.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.app_store.models.Admin;

public class AdminPrefManager {
    private static final String SHARED_PREF_NAME = "admin_pref";
    private static final String KEY_ID = "admin_id";
    private static final String KEY_NAME = "admin_name";
    private static final String KEY_EMAIL = "admin_email";

    private static AdminPrefManager instance;
    private final SharedPreferences sharedPreferences;

    private AdminPrefManager(Context context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized AdminPrefManager getInstance(Context context) {
        if (instance == null) {
            instance = new AdminPrefManager(context);
        }
        return instance;
    }

    public void saveAdmin(Admin admin) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_ID, admin.getId());
        editor.putString(KEY_NAME, admin.getName());
        editor.putString(KEY_EMAIL, admin.getEmail());
        editor.apply();
    }

    public Admin getAdmin() {
        return new Admin(
                sharedPreferences.getInt(KEY_ID, -1),
                sharedPreferences.getString(KEY_NAME, null),
                sharedPreferences.getString(KEY_EMAIL, null)
        );
    }

    public boolean isLoggedIn() {
        return sharedPreferences.contains(KEY_ID);
    }

    public void logout() {
        sharedPreferences.edit().clear().apply();
    }
}
