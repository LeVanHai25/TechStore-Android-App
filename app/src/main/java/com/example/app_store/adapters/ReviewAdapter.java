package com.example.app_store.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_store.R;
import com.example.app_store.models.Review;

import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private Context context;
    private List<Review> list;

    public ReviewAdapter(Context context, List<Review> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review r = list.get(position);
        holder.tvUserName.setText(r.getUser_name());
        holder.rbRating.setRating(r.getRating());
        holder.tvComment.setText(r.getComment());
        
        // Hiển thị admin reply nếu có (chỉ hiển thị nội dung, không có "Admin:")
        if (r.getAdmin_reply() != null && !r.getAdmin_reply().trim().isEmpty()) {
            holder.tvAdminReply.setText(r.getAdmin_reply());
            holder.tvAdminReply.setVisibility(View.VISIBLE);
        } else {
            holder.tvAdminReply.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvComment, tvAdminReply;
        RatingBar rbRating;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvAdminReply = itemView.findViewById(R.id.tvAdminReply);
            rbRating = itemView.findViewById(R.id.rb_review_rating);
        }
    }
}
