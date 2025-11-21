package com.example.app_store.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_store.R;
import com.example.app_store.models.User;
import com.example.app_store.models.SimpleResponse;
import com.example.app_store.network.APIService;
import com.example.app_store.network.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.ViewHolder> {

    private Context context;
    private List<User> userList;
    private APIService apiService;

    public AdminUserAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
        apiService = RetrofitClient.getInstance().create(APIService.class);
    }

    @NonNull
    @Override
    public AdminUserAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_admin, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminUserAdapter.ViewHolder holder, int position) {
        User user = userList.get(position);

        holder.tvName.setText(user.getName());
        holder.tvEmail.setText(user.getEmail());
        holder.tvRole.setText("Vai trò: " + user.getRole());

        // Hiển thị icon block/unblock dựa theo status
        if ("blocked".equalsIgnoreCase(user.getStatus())) {
            holder.btnBlock.setImageResource(android.R.drawable.ic_lock_idle_lock); // icon khoá
        } else {
            holder.btnBlock.setImageResource(R.drawable.ic_lock_idle_key); // icon mở
        }

        // Xử lý xoá
        holder.btnDelete.setOnClickListener(v -> {
            if ("admin".equalsIgnoreCase(user.getRole())) {
                Toast.makeText(context, "Không thể xoá tài khoản admin!", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(context)
                    .setTitle("Xác nhận xoá")
                    .setMessage("Bạn có chắc chắn muốn xoá người dùng \"" + user.getName() + "\" không?")
                    .setIcon(R.drawable.ic_delete) // icon tuỳ chỉnh
                    .setPositiveButton("Xoá", (dialog, which) -> deleteUser(user.getId(), position))
                    .setNegativeButton("Huỷ", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        // Xử lý chặn/mở chặn
        holder.btnBlock.setOnClickListener(v -> {
            if ("admin".equalsIgnoreCase(user.getRole())) {
                Toast.makeText(context, "Không thể chặn tài khoản admin!", Toast.LENGTH_SHORT).show();
                return;
            }

            String newStatus = "active".equalsIgnoreCase(user.getStatus()) ? "blocked" : "active";
            String message = "Bạn có chắc chắn muốn " +
                    (newStatus.equals("blocked") ? "chặn" : "mở chặn") +
                    " người dùng \"" + user.getName() + "\" không?";

            new AlertDialog.Builder(context)
                    .setTitle("Xác nhận")
                    .setMessage(message)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("Đồng ý", (dialog, which) -> updateUserStatus(user, newStatus, position))
                    .setNegativeButton("Huỷ", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    private void deleteUser(int userId, int position) {
        apiService.deleteUser(userId).enqueue(new Callback<SimpleResponse>() {
            @Override
            public void onResponse(Call<SimpleResponse> call, Response<SimpleResponse> response) {
                if (response.isSuccessful() && response.body().isSuccess()) {
                    userList.remove(position);
                    notifyItemRemoved(position);
                    Toast.makeText(context, "Đã xoá người dùng", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Không thể xoá", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SimpleResponse> call, Throwable t) {
                Toast.makeText(context, "Lỗi xoá người dùng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUserStatus(User user, String newStatus, int position) {
        apiService.updateUserStatus(user.getId(), newStatus).enqueue(new Callback<SimpleResponse>() {
            @Override
            public void onResponse(Call<SimpleResponse> call, Response<SimpleResponse> response) {
                if (response.isSuccessful() && response.body().isSuccess()) {
                    user.setStatus(newStatus);
                    notifyItemChanged(position);
                    Toast.makeText(context,
                            ("active".equals(newStatus) ? "Đã mở chặn" : "Đã chặn") + " " + user.getName(),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Không thể cập nhật trạng thái", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SimpleResponse> call, Throwable t) {
                Toast.makeText(context, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void filterList(List<User> filtered) {
        this.userList = filtered;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvRole;
        ImageButton btnDelete, btnBlock;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_user_name);
            tvEmail = itemView.findViewById(R.id.tv_user_email);
            tvRole = itemView.findViewById(R.id.tv_user_role);
            btnDelete = itemView.findViewById(R.id.btn_delete_user);
            btnBlock = itemView.findViewById(R.id.btn_block_user);
        }
    }
}
