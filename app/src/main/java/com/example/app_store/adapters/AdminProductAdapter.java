package com.example.app_store.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.app_store.R;
import com.example.app_store.admin.EditProductActivity;
import com.example.app_store.models.Product;
import com.example.app_store.models.SimpleResponse;
import com.example.app_store.network.APIClient;
import com.example.app_store.network.APIService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminProductAdapter extends RecyclerView.Adapter<AdminProductAdapter.ViewHolder> {

    private final Context context;
    private final List<Product> productList;
    private final String BASE_URL = "http://10.0.2.2/backend/images/"; // Đổi thành IP backend của bạn
    private final APIService apiService;

    public AdminProductAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
        this.apiService = APIClient.getClient().create(APIService.class);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product_admin, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product p = productList.get(position);

        holder.tvName.setText(p.getName());
        holder.tvPrice.setText(String.format("%,.0f₫", p.getPrice()));
        holder.tvCategory.setText(p.getCategory());

        Glide.with(context)
                .load(BASE_URL + p.getImage())
                .placeholder(R.drawable.placeholder)
                .into(holder.imgProduct);

        // Chỉnh sửa sản phẩm
        holder.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditProductActivity.class);
            intent.putExtra("product", p); // Gửi Product qua Intent
            context.startActivity(intent);
        });

        // Xoá sản phẩm
        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Xoá sản phẩm")
                    .setMessage("Bạn có chắc muốn xoá sản phẩm này?")
                    .setPositiveButton("Xoá", (dialog, which) -> deleteProduct(p.getId(), position))
                    .setNegativeButton("Huỷ", null)
                    .show();
        });
    }

    private void deleteProduct(int productId, int position) {
        // Tạo JSON body
        Map<String, Integer> body = new HashMap<>();
        body.put("id", productId);

        apiService.deleteProduct(body).enqueue(new Callback<SimpleResponse>() {
            @Override
            public void onResponse(Call<SimpleResponse> call, Response<SimpleResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    productList.remove(position);
                    notifyItemRemoved(position);
                    Toast.makeText(context, "Đã xoá sản phẩm", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Xoá thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SimpleResponse> call, Throwable t) {
                Toast.makeText(context, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("DELETE", "Error: " + t.getMessage());
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public void filterList(List<Product> filteredList) {
        productList.clear();
        productList.addAll(filteredList);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvCategory;
        ImageView imgProduct;
        ImageButton btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvPrice = itemView.findViewById(R.id.tv_price);
            tvCategory = itemView.findViewById(R.id.tv_category);
            imgProduct = itemView.findViewById(R.id.img_product);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
