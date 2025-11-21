package com.example.app_store.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_store.R;
import com.example.app_store.admin.AdminOrderDetailsActivity;
import com.example.app_store.models.Order;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AdminOrderAdapter extends RecyclerView.Adapter<AdminOrderAdapter.OrderViewHolder> {

    private final Context context;
    private final List<Order> orderList;

    public AdminOrderAdapter(Context context, List<Order> orderList) {
        this.context = context;
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_admin, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);

        holder.tvOrderId.setText("Mã đơn: #" + order.getId());
        holder.tvUser.setText("Người đặt: " + order.getUser_name());
        holder.tvDate.setText("Ngày: " + order.getCreated_at());
        holder.tvTotal.setText("Tổng: " + formatCurrency(order.getTotal()));
        holder.tvStatus.setText(order.getStatus());

        // Màu trạng thái
        switch (order.getStatus()) {
            case "Chờ xử lý":
                holder.tvStatus.setTextColor(Color.parseColor("#F59E0B")); // vàng
                break;
            case "Đang giao":
                holder.tvStatus.setTextColor(Color.parseColor("#3B82F6")); // xanh dương
                break;
            case "Đã giao":
                holder.tvStatus.setTextColor(Color.parseColor("#10B981")); // xanh lá
                break;
            case "Đã hủy":
                holder.tvStatus.setTextColor(Color.parseColor("#EF4444")); // đỏ
                break;
            case "Hoàn tiền":
                holder.tvStatus.setTextColor(Color.parseColor("#8B5CF6")); // tím
                break;
            default:
                holder.tvStatus.setTextColor(Color.DKGRAY);
                break;
        }

        // Click để mở chi tiết
        holder.cardOrder.setOnClickListener(v -> {
            Intent intent = new Intent(context, AdminOrderDetailsActivity.class);
            intent.putExtra("order", order);
            if (context instanceof Activity) {
                ((Activity) context).startActivityForResult(intent, 1001);
            }
        });
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvUser, tvDate, tvTotal, tvStatus;
        CardView cardOrder;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tv_order_id);
            tvUser = itemView.findViewById(R.id.tv_user);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvTotal = itemView.findViewById(R.id.tv_total);
            tvStatus = itemView.findViewById(R.id.tv_status);
            cardOrder = itemView.findViewById(R.id.card_order);
        }
    }

    private String formatCurrency(double amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return formatter.format(amount);
    }
}
