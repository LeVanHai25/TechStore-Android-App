package com.example.app_store.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_store.R;
import com.example.app_store.models.ReviewAdmin;

import java.util.List;

public class AdminReviewAdapter extends RecyclerView.Adapter<AdminReviewAdapter.Holder> {

    public interface ReviewListener {
        void onDelete(int id);
        void onReply(int id, String msg);
    }

    private Context ctx;
    private List<ReviewAdmin> list;
    private ReviewListener listener;

    public AdminReviewAdapter(Context ctx, List<ReviewAdmin> list, ReviewListener listener) {
        this.ctx = ctx;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_admin_review, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int p) {
        ReviewAdmin r = list.get(p);

        h.tvUser.setText(r.getUser_name());
        h.tvComment.setText(r.getComment());
        h.rb.setRating(r.getRating());

        if (r.getAdmin_reply() == null || r.getAdmin_reply().trim().isEmpty()) {
            h.tvReply.setText("Chưa phản hồi");
        } else {
            h.tvReply.setText("Admin: " + r.getAdmin_reply());
        }

        h.btnDelete.setOnClickListener(v -> listener.onDelete(r.getId()));

        h.btnReply.setOnClickListener(v -> {
            EditText edt = new EditText(ctx);
            edt.setHint("Nhập phản hồi (ít nhất 2 ký tự)...");

            AlertDialog dialog = new AlertDialog.Builder(ctx)
                    .setTitle("Phản hồi bình luận")
                    .setView(edt)
                    .setPositiveButton("Gửi", null) // Set null để tự xử lý
                    .setNegativeButton("Hủy", null)
                    .create();
            
            dialog.setOnShowListener(d -> {
                Button btnSend = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                btnSend.setOnClickListener(view -> {
                    String reply = edt.getText().toString().trim();
                    if (reply.length() < 2) {
                        edt.setError("Phản hồi phải có ít nhất 2 ký tự");
                        edt.requestFocus();
                    } else {
                        listener.onReply(r.getId(), reply);
                        dialog.dismiss();
                    }
                });
            });
            
            dialog.show();
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView tvUser, tvComment, tvReply;
        RatingBar rb;
        Button btnDelete, btnReply;

        public Holder(@NonNull View v) {
            super(v);

            tvUser = v.findViewById(R.id.tvUser);
            tvComment = v.findViewById(R.id.tvComment);
            tvReply = v.findViewById(R.id.tvAdminReply);
            rb = v.findViewById(R.id.rbRating);

            btnDelete = v.findViewById(R.id.btnDelete);
            btnReply = v.findViewById(R.id.btnReply);
        }
    }
}
