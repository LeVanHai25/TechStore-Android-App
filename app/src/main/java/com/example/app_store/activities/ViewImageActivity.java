package com.example.app_store.activities;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.app_store.R;

public class ViewImageActivity extends AppCompatActivity {

    private ImageView imgFull;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_image);

        imgFull = findViewById(R.id.img_fullscreen);

        String imageUrl = getIntent().getStringExtra("image"); // link đã gửi từ ProductDetailActivity

        Glide.with(this)
                .load(imageUrl)
                .into(imgFull);

        imgFull.setOnClickListener(v -> finish()); // chạm thoát
    }
}
