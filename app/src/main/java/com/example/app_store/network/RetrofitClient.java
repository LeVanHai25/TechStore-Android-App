package com.example.app_store.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit;
    private static final String BASE_URL = "http://10.0.2.2/backend/";  // 👉 đổi theo IP backend của bạn

    // Khởi tạo Retrofit nếu chưa tồn tại
    public static Retrofit getInstance() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    // Trả về instance của APIService
    public static APIService getAPIService() {
        return getInstance().create(APIService.class);
    }
}
