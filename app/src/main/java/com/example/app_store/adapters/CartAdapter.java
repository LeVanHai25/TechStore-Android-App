package com.example.app_store.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.app_store.R;
import com.example.app_store.models.CartItem;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private Context context;
    private List<CartItem> cartItems;
    private CartUpdateListener listener;
    private final String BASE_IMAGE_URL = "http://10.0.2.2/backend/images/"; // Đổi theo IP backend của bạn

    public interface CartUpdateListener {
        void onQuantityChanged(CartItem item, int newQuantity);
        void onItemDeleted(CartItem item);
    }

    public CartAdapter(Context context, List<CartItem> cartItems, CartUpdateListener listener) {
        this.context = context;
        this.cartItems = cartItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        if (item.getProduct() == null) return;

        holder.tvName.setText(item.getProduct().getName());
        double itemPrice = item.getPrice() > 0 ? item.getPrice() : item.getProduct().getPrice();
        holder.tvPrice.setText(String.format("%,.0f₫", itemPrice));

        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));

        Glide.with(context)
                .load(BASE_IMAGE_URL + item.getProduct().getImage())
                .placeholder(R.drawable.placeholder)
                .into(holder.imgProduct);

        // ✅ Tăng số lượng
        holder.btnIncrease.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            CartItem clickedItem = cartItems.get(pos);
            listener.onQuantityChanged(clickedItem, clickedItem.getQuantity() + 1);
        });

        // ✅ Giảm số lượng
        holder.btnDecrease.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            CartItem clickedItem = cartItems.get(pos);
            if (clickedItem.getQuantity() > 1) {
                listener.onQuantityChanged(clickedItem, clickedItem.getQuantity() - 1);
            } else {
                Toast.makeText(context, "Số lượng tối thiểu là 1", Toast.LENGTH_SHORT).show();
            }
        });

        // ✅ Xóa sản phẩm
        // ✅ Xóa sản phẩm (hộp thoại hệ thống)
        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            CartItem clickedItem = cartItems.get(pos);

            new android.app.AlertDialog.Builder(context)
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc muốn xóa sản phẩm \""
                            + clickedItem.getProduct().getName() + "\" khỏi giỏ hàng không?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        listener.onItemDeleted(clickedItem);
                        dialog.dismiss();
                    })
                    .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                    .show();
        });

    }


    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView tvName, tvPrice, tvQuantity;
        ImageView btnIncrease, btnDecrease, btnDelete;


        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.img_product_cart);
            tvName = itemView.findViewById(R.id.tv_cart_name);
            tvPrice = itemView.findViewById(R.id.tv_cart_price);
            tvQuantity = itemView.findViewById(R.id.tv_cart_quantity);
            btnIncrease = itemView.findViewById(R.id.btn_increase);
            btnDecrease = itemView.findViewById(R.id.btn_decrease);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
