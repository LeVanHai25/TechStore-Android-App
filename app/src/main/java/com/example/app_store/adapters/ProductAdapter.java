package com.example.app_store.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.app_store.R;
import com.example.app_store.activities.ProductDetailActivity;
import com.example.app_store.models.Product;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private final Context context;
    private final List<Product> productList;
    private final String BASE_URL = "http://10.0.2.2/backend/images/"; // Đổi theo IP backend của bạn

    public ProductAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product p = productList.get(position);

        // Set dữ liệu sản phẩm
        holder.tvName.setText(p.getName());
        holder.tvPrice.setText(String.format("%,.0f₫", p.getPrice()));
        holder.tvCategory.setText(p.getCategory() != null ? p.getCategory() : "Không rõ");

        // Load ảnh bằng Glide
        Glide.with(context)
                .load(BASE_URL + p.getImage())
                .placeholder(R.drawable.placeholder)
                .into(holder.imgProduct);

        // Mở chi tiết sản phẩm khi click
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetailActivity.class);
            intent.putExtra("product", p);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    // ViewHolder
    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvCategory;
        ImageView imgProduct;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName     = itemView.findViewById(R.id.tv_name);
            tvPrice    = itemView.findViewById(R.id.tv_price);
            tvCategory = itemView.findViewById(R.id.tv_category);
            imgProduct = itemView.findViewById(R.id.img_product);
        }
    }
}
