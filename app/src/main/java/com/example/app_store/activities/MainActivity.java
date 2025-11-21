package com.example.app_store.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_store.R;
import com.example.app_store.adapters.ProductAdapter;
import com.example.app_store.models.Product;
import com.example.app_store.models.ProductResponse;
import com.example.app_store.models.User;
import com.example.app_store.network.APIService;
import com.example.app_store.network.RetrofitClient;
import com.example.app_store.utils.SharedPrefManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private APIService apiService;

    private EditText edtSearch;
    private TextView tvGreeting, tvUsername;
    private ImageView imgLogo;
    private List<TextView> categories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --------------------- ÁNH XẠ VIEW ---------------------
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        recyclerView = findViewById(R.id.recycler_products);
        edtSearch = findViewById(R.id.edt_search);
        tvGreeting = findViewById(R.id.tv_greeting);
        tvUsername = findViewById(R.id.tv_username);
        imgLogo = findViewById(R.id.img_logo);

        TextView catLaptop = findViewById(R.id.category_laptop);
        TextView catMouse = findViewById(R.id.category_mouse);
        TextView catPhone = findViewById(R.id.category_phone);
        TextView catHeadphone = findViewById(R.id.category_headphone);
        categories = Arrays.asList(catLaptop, catMouse, catPhone, catHeadphone);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        apiService = RetrofitClient.getAPIService();

        // --------------------- KIỂM TRA ĐĂNG NHẬP ---------------------
        if (!SharedPrefManager.getInstance(this).isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        User currentUser = SharedPrefManager.getInstance(this).getUser();
        if (currentUser != null) {
            tvUsername.setText(currentUser.getName() + "!");
        } else {
            tvUsername.setText("Khách!");
        }

        // --------------------- LOGO VÀO ACCOUNT ---------------------
        imgLogo.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AccountActivity.class))
        );

        // --------------------- XỬ LÝ DANH MỤC ---------------------
        setCategoryListeners();

        // Mặc định hiển thị Laptop
        highlightCategory(catLaptop);
        loadProductsByCategory("Laptop");

        // --------------------- XỬ LÝ THANH TÌM KIẾM ---------------------
        edtSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                String keyword = edtSearch.getText().toString().trim();
                if (!keyword.isEmpty()) {
                    searchProducts(keyword);
                } else {
                    Toast.makeText(this, "Vui lòng nhập từ khóa tìm kiếm", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });

        // --------------------- XỬ LÝ BOTTOM NAV ---------------------
        bottomNav.setOnItemSelectedListener(item -> {
            // Hiệu ứng phóng to icon khi chọn
            View iconView = bottomNav.findViewById(item.getItemId());
            if (iconView != null) {
                iconView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_up));
            }

            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                return true;
            }
            else if (itemId == R.id.nav_cart) {
                startActivity(new Intent(MainActivity.this, CartActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            }
            else if (itemId == R.id.nav_order) {
                startActivity(new Intent(MainActivity.this, OrderHistoryActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            }
            else if (itemId == R.id.nav_account) {
                startActivity(new Intent(MainActivity.this, AccountActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            }
            else {
                return false;
            }
        });

    }

    // --------------------- XỬ LÝ DANH MỤC ---------------------
    private void setCategoryListeners() {
        Runnable resetAll = () -> {
            for (TextView tv : categories) {
                tv.setBackgroundResource(R.drawable.bg_category_default);
                tv.setTextColor(getResources().getColor(R.color.titleColor));
            }
        };

        for (TextView tv : categories) {
            tv.setOnClickListener(v -> {
                resetAll.run();
                highlightCategory(tv);
                loadProductsByCategory(tv.getText().toString());
            });
        }
    }

    private void highlightCategory(TextView selected) {
        selected.setBackgroundResource(R.drawable.bg_category_selected);
        selected.setTextColor(Color.WHITE);
    }

    // --------------------- GỌI API LẤY SẢN PHẨM ---------------------
    private void loadProductsByCategory(String category) {
        apiService.getProductsByCategory(category).enqueue(new Callback<ProductResponse>() {
            @Override
            public void onResponse(Call<ProductResponse> call, Response<ProductResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Product> products = response.body().getProducts();
                    adapter = new ProductAdapter(MainActivity.this, products);
                    recyclerView.setAdapter(adapter);
                } else {
                    Toast.makeText(MainActivity.this, "Không có sản phẩm trong danh mục này", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ProductResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchProducts(String keyword) {
        apiService.searchProducts(keyword).enqueue(new Callback<ProductResponse>() {
            @Override
            public void onResponse(Call<ProductResponse> call, Response<ProductResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Product> products = response.body().getProducts();
                    adapter = new ProductAdapter(MainActivity.this, products);
                    recyclerView.setAdapter(adapter);
                } else {
                    Toast.makeText(MainActivity.this, "Không tìm thấy sản phẩm phù hợp", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ProductResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --------------------- MENU LOGOUT ---------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_logout) {
            SharedPrefManager.getInstance(this).logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
