package com.example.app_store.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_store.R;
import com.example.app_store.adapters.CartAdapter;
import com.example.app_store.models.CartItem;
import com.example.app_store.models.CartRequest;
import com.example.app_store.models.CartResponse;
import com.example.app_store.models.GeneralResponse;
import com.example.app_store.models.OrderRequest;
import com.example.app_store.models.Promotion;
import com.example.app_store.models.PromoResponse;
import com.example.app_store.models.User;
import com.example.app_store.network.APIService;
import com.example.app_store.network.RetrofitClient;
import com.example.app_store.utils.SharedPrefManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartActivity extends AppCompatActivity {

    private RecyclerView recyclerCart;
    private TextView tvTotalPrice, tvOriginalPrice, tvDiscount;
    private Button btnCheckout, btnPromotion;

    private CartAdapter cartAdapter;
    private List<CartItem> cartItems = new ArrayList<>();
    private APIService apiService;
    private User currentUser;
    
    // Khuyến mãi
    private Promotion currentPromotion;
    private double originalTotal = 0;
    private double discountAmount = 0;
    private double finalTotal = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        // ---------------- ÁNH XẠ VIEW ----------------
        recyclerCart = findViewById(R.id.recycler_cart);
        tvTotalPrice = findViewById(R.id.tv_total_price);
        tvOriginalPrice = findViewById(R.id.tv_original_price);
        tvDiscount = findViewById(R.id.tv_discount);
        btnCheckout = findViewById(R.id.btn_checkout);
        btnPromotion = findViewById(R.id.btn_promotion);
        ImageButton btnBack = findViewById(R.id.btn_back);

        recyclerCart.setLayoutManager(new LinearLayoutManager(this));
        apiService = RetrofitClient.getAPIService();
        currentUser = SharedPrefManager.getInstance(this).getUser();

        // ---------------- NÚT QUAY LẠI ----------------
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(CartActivity.this, MainActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        // ---------------- KHỞI TẠO ADAPTER ----------------
        cartAdapter = new CartAdapter(this, cartItems, new CartAdapter.CartUpdateListener() {
            @Override
            public void onQuantityChanged(CartItem item, int newQuantity) {
                updateCart(item.getProduct().getId(), newQuantity);
            }

            @Override
            public void onItemDeleted(CartItem item) {
                deleteCartItem(item.getProduct().getId());
            }
        });

        recyclerCart.setAdapter(cartAdapter);

        loadCart();

        // ---------------- XỬ LÝ THANH TOÁN ----------------
        btnCheckout.setOnClickListener(v -> showCheckoutDialog());

        // ---------------- THANH ĐIỀU HƯỚNG DƯỚI ----------------
        setupBottomNavigation();
    }

    // ---------------- HỘP THOẠI THANH TOÁN ----------------
    private void showCheckoutDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_checkout, null);

        EditText edtAddress = view.findViewById(R.id.edt_address);
        EditText edtPhone = view.findViewById(R.id.edt_phone);
        EditText edtNote = view.findViewById(R.id.edt_note);
        Button btnConfirm = view.findViewById(R.id.btn_confirm_checkout);

        // ⭐ TỰ ĐỘNG ĐIỀN ĐỊA CHỈ + SĐT CỦA USER
        if (currentUser != null) {
            edtAddress.setText(currentUser.getAddress());
            edtPhone.setText(currentUser.getPhone());
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        btnConfirm.setOnClickListener(v -> {
            String address = edtAddress.getText().toString().trim();
            String phone = edtPhone.getText().toString().trim();
            String note = edtNote.getText().toString().trim();

            if (address.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ địa chỉ và số điện thoại", Toast.LENGTH_SHORT).show();
                return;
            }

            // QUAN TRỌNG: Gửi tổng tiền sau khi áp dụng khuyến mãi (nếu có)
            double orderTotal = (currentPromotion != null) ? finalTotal : originalTotal;
            
            OrderRequest request = new OrderRequest(currentUser.getId(), address, phone, note, orderTotal);

            apiService.placeOrder(request).enqueue(new Callback<GeneralResponse>() {
                @Override
                public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Toast.makeText(CartActivity.this, "Đặt hàng thành công!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadCart();
                    } else {
                        Toast.makeText(CartActivity.this, "Không thể đặt hàng", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<GeneralResponse> call, Throwable t) {
                    Toast.makeText(CartActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }


    // ---------------- XÓA SẢN PHẨM ----------------
    private void deleteCartItem(int productId) {
        apiService.deleteFromCart(currentUser.getId(), productId).enqueue(new Callback<GeneralResponse>() {
            @Override
            public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(CartActivity.this, "Đã xóa sản phẩm", Toast.LENGTH_SHORT).show();
                    loadCart();
                } else {
                    Toast.makeText(CartActivity.this, "Không thể xóa sản phẩm", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GeneralResponse> call, Throwable t) {
                Toast.makeText(CartActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---------------- TẢI GIỎ HÀNG ----------------
    private void loadCart() {
        apiService.getCart(currentUser.getId()).enqueue(new Callback<CartResponse>() {
            @Override
            public void onResponse(Call<CartResponse> call, Response<CartResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    cartItems.clear();
                    // Kiểm tra null trước khi addAll
                    List<CartItem> items = response.body().getCartItems();
                    if (items != null) {
                        cartItems.addAll(items);
                    }
                    cartAdapter.notifyDataSetChanged();
                    updateTotalPrice();
                    checkPromotion(); // Kiểm tra khuyến mãi sau khi load cart
                } else {
                    Toast.makeText(CartActivity.this, "Không thể tải giỏ hàng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CartResponse> call, Throwable t) {
                Toast.makeText(CartActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---------------- CẬP NHẬT SỐ LƯỢNG ----------------
    private void updateCart(int productId, int newQuantity) {
        double price = 0;
        for (CartItem item : cartItems) {
            if (item.getProduct().getId() == productId) {
                price = item.getPrice() > 0 ? item.getPrice() : item.getProduct().getPrice();
                break;
            }
        }
        CartRequest request = new CartRequest(currentUser.getId(), productId, newQuantity, price);
        apiService.updateCart(request).enqueue(new Callback<GeneralResponse>() {
            @Override
            public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    loadCart();
                } else {
                    Toast.makeText(CartActivity.this, "Không thể cập nhật giỏ hàng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GeneralResponse> call, Throwable t) {
                Toast.makeText(CartActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---------------- TỔNG TIỀN ----------------
    private void updateTotalPrice() {
        double total = 0;
        for (CartItem item : cartItems) {
            double itemPrice;

            if (item.getPrice() > 0) {
                // dùng giá trong cart (đã cộng thêm)
                itemPrice = item.getPrice();
            } else {
                // fallback: dùng giá product nếu cart chưa có field price
                itemPrice = item.getProduct().getPrice();
            }

            total += itemPrice * item.getQuantity();
        }

        originalTotal = total;
        
        // Nếu đã áp dụng khuyến mãi, tính lại
        if (currentPromotion != null) {
            discountAmount = originalTotal * currentPromotion.getDiscount_percent() / 100.0;
            finalTotal = originalTotal - discountAmount;
            
            // Hiển thị giá gốc (gạch ngang) - set strikethrough programmatically
            tvOriginalPrice.setText(String.format("%,.0f₫", originalTotal));
            tvOriginalPrice.setPaintFlags(tvOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            tvOriginalPrice.setVisibility(View.VISIBLE);
            
            // Hiển thị giảm giá
            tvDiscount.setText(String.format("-%,.0f₫ (Giảm %d%%)", discountAmount, currentPromotion.getDiscount_percent()));
            tvDiscount.setVisibility(View.VISIBLE);
            
            // Hiển thị giá sau giảm
            tvTotalPrice.setText(String.format("%,.0f₫", finalTotal));
        } else {
            // Không có khuyến mãi
            tvOriginalPrice.setVisibility(View.GONE);
            tvDiscount.setVisibility(View.GONE);
            // Remove strikethrough when no promotion
            tvOriginalPrice.setPaintFlags(tvOriginalPrice.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
            tvTotalPrice.setText(String.format("%,.0f₫", originalTotal));
            finalTotal = originalTotal;
        }
    }

    // ---------------- KIỂM TRA KHUYẾN MÃI ----------------
    private void checkPromotion() {
        if (originalTotal == 0) {
            btnPromotion.setVisibility(View.GONE);
            return;
        }

        apiService.getBestPromotion((long) originalTotal).enqueue(new Callback<PromoResponse>() {
            @Override
            public void onResponse(Call<PromoResponse> call, Response<PromoResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Promotion promo = response.body().getPromotion();
                    if (promo != null) {
                        currentPromotion = promo;
                        btnPromotion.setVisibility(View.VISIBLE);
                        btnPromotion.setText("🎁 Bạn đang có ưu đãi " + promo.getDiscount_percent() + "%!");
                        
                        btnPromotion.setOnClickListener(v -> showPromoDialog(promo));
                    } else {
                        btnPromotion.setVisibility(View.GONE);
                        currentPromotion = null;
                    }
                } else {
                    btnPromotion.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<PromoResponse> call, Throwable t) {
                // Lỗi không ảnh hưởng, chỉ ẩn nút
                btnPromotion.setVisibility(View.GONE);
            }
        });
    }

    // ---------------- POPUP ÁP DỤNG KHUYẾN MÃI ----------------
    private void showPromoDialog(Promotion promo) {
        double discount = originalTotal * promo.getDiscount_percent() / 100.0;
        double finalPrice = originalTotal - discount;

        String message = promo.getDescription() + 
                        "\n\n" +
                        "Tổng tiền: " + String.format("%,.0f₫", originalTotal) +
                        "\nGiảm: " + promo.getDiscount_percent() + "% (-" + String.format("%,.0f₫", discount) + ")" +
                        "\n\nThành tiền: " + String.format("%,.0f₫", finalPrice);

        new AlertDialog.Builder(this)
                .setTitle("🎁 Ưu đãi của bạn")
                .setMessage(message)
                .setPositiveButton("Áp dụng", (d, w) -> {
                    applyPromotion(promo);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ---------------- ÁP DỤNG KHUYẾN MÃI ----------------
    private void applyPromotion(Promotion promo) {
        currentPromotion = promo;
        updateTotalPrice(); // Cập nhật lại tổng tiền với khuyến mãi
        btnPromotion.setText("✅ Đã áp dụng " + promo.getDiscount_percent() + "%");
        Toast.makeText(this, "Đã áp dụng ưu đãi " + promo.getDiscount_percent() + "%", Toast.LENGTH_SHORT).show();
    }


    // ---------------- THANH ĐIỀU HƯỚNG ----------------
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_cart);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(CartActivity.this, MainActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
                return true;
            } else if (id == R.id.nav_cart) {
                return true;
            } else if (id == R.id.nav_order) {
                startActivity(new Intent(CartActivity.this, OrderHistoryActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
                return true;
            } else if (id == R.id.nav_account) {
                startActivity(new Intent(CartActivity.this, AccountActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
                return true;
            }
            return false;
        });
    }
}
