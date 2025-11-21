package com.example.app_store.admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.database.Cursor;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.app_store.R;
import com.example.app_store.models.Product;
import com.example.app_store.models.SimpleResponse;
import com.example.app_store.models.UploadResponse;
import com.example.app_store.network.APIService;
import com.example.app_store.network.RetrofitClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProductActivity extends AppCompatActivity {

    private static final String BASE_IMAGE_URL = "http://10.0.2.2/backend/images/";

    EditText edtName, edtDesc, edtPrice, edtCategory;
    ImageView imgPreview;
    Button btnChooseImage, btnSave;
    Uri selectedImageUri;
    String uploadedImageName = null;

    private boolean isEditMode = false;
    private Product existingProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_product);

        // Ánh xạ view
        edtName     = findViewById(R.id.et_name);
        edtDesc     = findViewById(R.id.et_description);
        edtPrice    = findViewById(R.id.et_price);
        edtCategory = findViewById(R.id.et_category);
        imgPreview  = findViewById(R.id.img_preview);
        btnChooseImage = findViewById(R.id.btn_select_image);
        btnSave        = findViewById(R.id.btn_save);
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Kiểm tra chế độ sửa
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("product")) {
            isEditMode = true;
            existingProduct = (Product) intent.getSerializableExtra("product");
            fillProductData(existingProduct);
        }

        // Chọn ảnh
        btnChooseImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        // Lưu sản phẩm
        btnSave.setOnClickListener(v -> saveProduct());
    }

    // Hiển thị dữ liệu cũ
    private void fillProductData(Product product) {
        edtName.setText(product.getName());
        edtDesc.setText(product.getDescription());
        edtPrice.setText(String.valueOf(product.getPrice()));
        edtCategory.setText(product.getCategory());
        uploadedImageName = product.getImage();

        if (uploadedImageName != null && !uploadedImageName.isEmpty()) {
            Glide.with(this)
                    .load(BASE_IMAGE_URL + uploadedImageName)
                    .placeholder(R.drawable.placeholder)
                    .into(imgPreview);
        }
    }

    // Image Picker
    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    imgPreview.setImageURI(uri);
                    uploadImageToServer(uri);
                }
            });

    // Upload ảnh
    private void uploadImageToServer(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            String fileName = System.currentTimeMillis() + ".jpg";
            File tempFile = new File(getCacheDir(), fileName);
            FileOutputStream outputStream = new FileOutputStream(tempFile);

            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }
            outputStream.close();
            inputStream.close();

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), tempFile);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", tempFile.getName(), requestFile);

            APIService apiService = RetrofitClient.getInstance().create(APIService.class);
            apiService.uploadImage(body).enqueue(new Callback<UploadResponse>() {
                @Override
                public void onResponse(Call<UploadResponse> call, Response<UploadResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        uploadedImageName = response.body().getFilename();
                        Toast.makeText(EditProductActivity.this, "Upload ảnh thành công!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(EditProductActivity.this, "Upload ảnh thất bại!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<UploadResponse> call, Throwable t) {
                    Toast.makeText(EditProductActivity.this, "Lỗi upload: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Không thể đọc ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveProduct() {
        String name     = edtName.getText().toString().trim();
        String desc     = edtDesc.getText().toString().trim();
        String priceStr = edtPrice.getText().toString().trim();
        String category = edtCategory.getText().toString().trim();

        if (name.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên và giá sản phẩm", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = Double.parseDouble(priceStr);
        String image = (uploadedImageName != null) ? uploadedImageName : "";

        if (isEditMode && existingProduct != null) {
            Product updatedProduct = new Product(
                    existingProduct.getId(),
                    name,
                    desc,
                    image,
                    price,
                    category,
                    existingProduct.getCreatedAt()
            );
            updateProduct(updatedProduct);
        } else {
            Product newProduct = new Product(0, name, desc, image, price, category, "");
            createProduct(newProduct);
        }
    }

    private void createProduct(Product product) {
        APIService apiService = RetrofitClient.getInstance().create(APIService.class);
        apiService.addProduct(product).enqueue(new Callback<SimpleResponse>() {
            @Override
            public void onResponse(Call<SimpleResponse> call, Response<SimpleResponse> response) {
                if (response.isSuccessful() && response.body().isSuccess()) {
                    Toast.makeText(EditProductActivity.this, "Thêm sản phẩm thành công", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditProductActivity.this, "Thêm thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SimpleResponse> call, Throwable t) {
                Toast.makeText(EditProductActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateProduct(Product product) {
        APIService apiService = RetrofitClient.getInstance().create(APIService.class);
        apiService.updateProduct(product).enqueue(new Callback<SimpleResponse>() {
            @Override
            public void onResponse(Call<SimpleResponse> call, Response<SimpleResponse> response) {
                if (response.isSuccessful() && response.body().isSuccess()) {
                    Toast.makeText(EditProductActivity.this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditProductActivity.this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SimpleResponse> call, Throwable t) {
                Toast.makeText(EditProductActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
