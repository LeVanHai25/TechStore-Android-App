package com.example.app_store.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_store.R;
import com.example.app_store.models.Promotion;

import java.util.List;

public class PromotionAdapter extends RecyclerView.Adapter<PromotionAdapter.PromotionViewHolder> {

    public interface PromotionListener {
        void onEdit(Promotion promotion);
        void onDelete(int id);
    }

    private Context context;
    private List<Promotion> promotionList;
    private PromotionListener listener;

    public PromotionAdapter(Context context, List<Promotion> promotionList, PromotionListener listener) {
        this.context = context;
        this.promotionList = promotionList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PromotionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_promotion, parent, false);
        return new PromotionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PromotionViewHolder holder, int position) {
        Promotion promotion = promotionList.get(position);

        holder.tvDiscount.setText("Giảm " + promotion.getDiscount_percent() + "%");
        holder.tvMinAmount.setText("Cho đơn hàng trên " + String.format("%,d₫", promotion.getMin_amount()));
        holder.tvDescription.setText(promotion.getDescription() != null ? promotion.getDescription() : "");

        holder.btnEdit.setOnClickListener(v -> listener.onEdit(promotion));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(promotion.getId()));
    }

    @Override
    public int getItemCount() {
        return promotionList.size();
    }

    static class PromotionViewHolder extends RecyclerView.ViewHolder {
        TextView tvDiscount, tvMinAmount, tvDescription;
        Button btnEdit, btnDelete;

        public PromotionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDiscount = itemView.findViewById(R.id.tvDiscount);
            tvMinAmount = itemView.findViewById(R.id.tvMinAmount);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}


