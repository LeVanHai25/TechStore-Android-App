package com.example.app_store.network;

import com.example.app_store.models.AddReviewRequest;
import com.example.app_store.models.AdminReviewResponse;
import com.example.app_store.models.CartRequest;
import com.example.app_store.models.CartResponse;
import com.example.app_store.models.ForgotPasswordRequest;
import com.example.app_store.models.GeneralResponse;
import com.example.app_store.models.LoginRequest;
import com.example.app_store.models.LoginResponse;
import com.example.app_store.models.OrderListResponse;
import com.example.app_store.models.OrderRequest;
import com.example.app_store.models.OrderResponse;
import com.example.app_store.models.Product;
import com.example.app_store.models.ProductListResponse;
import com.example.app_store.models.ProductResponse;
import com.example.app_store.models.Promotion;
import com.example.app_store.models.PromotionListResponse;
import com.example.app_store.models.PromoResponse;
import com.example.app_store.models.RegisterRequest;
import com.example.app_store.models.RevenueResponse;
import com.example.app_store.models.ReviewListResponse;
import com.example.app_store.models.SimpleResponse;
import com.example.app_store.models.StatusStatResponse;
import com.example.app_store.models.UploadResponse;
import com.example.app_store.models.User;
import com.example.app_store.models.UserListResponse;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface APIService {

    // Đăng nhập
    @Headers("Content-Type: application/json")
    @POST("users/loginUser.php")
    Call<LoginResponse> loginUser(@Body LoginRequest request);

    // Đăng ký
    @Headers("Content-Type: application/json")
    @POST("users/register.php")
    Call<GeneralResponse> registerUser(@Body RegisterRequest request);

    // Quên mật khẩu
    @Headers("Content-Type: application/json")
    @POST("users/forgot_password.php")
    Call<GeneralResponse> forgotPassword(@Body ForgotPasswordRequest request);

    // Lấy danh sách sản phẩm
    // Lấy danh sách sản phẩm có phân trang

//    @GET("products/list.php")
//    Call<ProductResponse> getAllProducts();
    @GET("products/get_products_by_category.php")
    Call<ProductResponse> getProductsByCategory(@Query("category") String category);
    @GET("products/search.php")
    Call<ProductResponse> searchProducts(@Query("keyword") String keyword);
    @POST("users/update_profile.php")
    Call<GeneralResponse> updateUser(@Body User user);
    // Thêm vào APIService.java

    @Headers("Content-Type: application/json")
    @POST("cart/add.php")
    Call<GeneralResponse> addToCart(@Body CartRequest request);

    @GET("cart/view.php")
    Call<CartResponse> getCart(@Query("user_id") int userId);

    @Headers("Content-Type: application/json")
    @POST("cart/update.php")
    Call<GeneralResponse> updateCart(@Body CartRequest request);
    @GET("cart/delete.php")
    Call<GeneralResponse> deleteFromCart(@Query("user_id") int userId, @Query("product_id") int productId);
    @Headers("Content-Type: application/json")
    @POST("orders/place_order.php")
    Call<GeneralResponse> placeOrder(@Body OrderRequest request);
    @GET("orders/list_user.php")
    Call<OrderResponse> getOrders(@Query("user_id") int userId);

    @POST("orders/cancel_order.php")
    @Headers("Content-Type: application/json")
    Call<GeneralResponse> cancelOrder(@Body Map<String, Integer> requestBody);
    @GET("products/list.php")
    Call<ProductListResponse> getAllProducts(
            @Query("page") int page,
            @Query("limit") int limit
    );


    @POST("products/add.php")
    Call<SimpleResponse> addProduct(@Body Product product);

    @POST("products/update.php")
    Call<SimpleResponse> updateProduct(@Body Product product);

    @POST("products/delete.php")
    Call<SimpleResponse> deleteProduct(@Body Map<String, Integer> idMap);

    @Multipart
    @POST("products/upload.php")
    Call<UploadResponse> uploadImage(@Part MultipartBody.Part image);
    @GET("admin/getAllUsers.php")
    Call<UserListResponse> getAllUsers();

    @POST("admin/deleteUser.php")
    Call<SimpleResponse> deleteUser(@Query("id") int userId);
    @GET("orders/getAllOrders.php")
    Call<OrderListResponse> getAllOrders();
    @FormUrlEncoded
    @POST("orders/updateOrderStatus.php")
    Call<SimpleResponse> updateOrderStatus(
            @Field("order_id") int orderId,
            @Field("new_status") String status // <-- sửa ở đây
    );
    @FormUrlEncoded
    @POST("orders/order_status_summary.php")
    Call<StatusStatResponse> getStatusStatsFiltered(
            @Field("day") int day,
            @Field("month") int month,
            @Field("year") int year
    );


    @GET("orders/statistics_by_filter.php")
    Call<RevenueResponse> getRevenueFiltered(
            @Query("day") int day,
            @Query("month") int month,
            @Query("year") int year
    );
    @FormUrlEncoded
    @POST("admin/blockUser.php")
    Call<SimpleResponse> blockUser(@Field("id") int userId);

    @FormUrlEncoded
    @POST("admin/unblockUser.php")
    Call<SimpleResponse> unblockUser(@Field("id") int userId);
    @FormUrlEncoded
    @POST("users/update_status.php")
    Call<SimpleResponse> updateUserStatus(
            @Field("user_id") int userId,
            @Field("status") String status
    );
    @FormUrlEncoded
    @POST("users/change_password.php")
    Call<GeneralResponse> changePassword(@FieldMap Map<String, String> params);

    // Lấy danh sách đánh giá
    @GET("reviews/get_reviews.php")
    Call<ReviewListResponse> getReviews(@Query("product_id") int productId);

    // Thêm đánh giá mới
    @POST("reviews/add_review.php")
    Call<GeneralResponse> addReview(@Body AddReviewRequest request);

    // Lấy danh sách bình luận cho admin
    @GET("reviews_admin/get_all.php")
    Call<AdminReviewResponse> getAllAdminReviews();


    // Xóa bình luận
    @GET("reviews_admin/delete.php")
    Call<GeneralResponse> deleteReview(@Query("id") int id);

    // Admin trả lời bình luận
    @FormUrlEncoded
    @POST("reviews_admin/reply.php")
    Call<GeneralResponse> replyReview(
            @Field("id") int id,
            @Field("reply") String reply
    );

    // ======================= PROMOTIONS =======================
    // Lấy tất cả khuyến mãi
    @GET("promotions/get_all.php")
    Call<PromotionListResponse> getPromotions();

    // Thêm khuyến mãi
    @Headers("Content-Type: application/json")
    @POST("promotions/add.php")
    Call<SimpleResponse> addPromotion(@Body Promotion promotion);

    // Cập nhật khuyến mãi
    @Headers("Content-Type: application/json")
    @POST("promotions/update.php")
    Call<SimpleResponse> updatePromotion(@Body Promotion promotion);

    // Xóa khuyến mãi
    @GET("promotions/delete.php")
    Call<SimpleResponse> deletePromotion(@Query("id") int id);

    // Lấy khuyến mãi tốt nhất theo tổng tiền
    @GET("promotions/get_best_promotion.php")
    Call<PromoResponse> getBestPromotion(@Query("total") long total);























}
