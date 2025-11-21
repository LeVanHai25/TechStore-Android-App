# Sửa lỗi: Admin reply không lưu được

## Vấn đề:
- Admin ấn "Trả lời" nhưng `admin_reply` vẫn NULL
- Phản hồi không hiển thị

## Nguyên nhân có thể:
1. ❌ File `reviews_admin/reply.php` không update đúng
2. ❌ File `reviews_admin/get_all.php` không SELECT `admin_reply`
3. ❌ Thiếu error handling trong Android

---

## File PHP cần sửa:

### 1. **reviews_admin/reply.php** (QUAN TRỌNG)

```php
<?php
header("Content-Type: application/json");
require_once "../config/config.php";

// Lấy dữ liệu từ POST
$id = $_POST["id"] ?? 0;
$reply = $_POST["reply"] ?? "";

// Debug: Kiểm tra dữ liệu nhận được
// error_log("Reply ID: " . $id);
// error_log("Reply Message: " . $reply);

if ($id == 0) {
    echo json_encode([
        "success" => false,
        "message" => "ID không hợp lệ"
    ]);
    exit;
}

try {
    // QUAN TRỌNG: Update vào bảng product_reviews
    $stmt = $conn->prepare("
        UPDATE product_reviews 
        SET admin_reply = ? 
        WHERE id = ?
    ");
    
    $ok = $stmt->execute([$reply, $id]);
    
    // Kiểm tra số dòng bị ảnh hưởng
    $affected = $stmt->rowCount();
    
    if ($ok && $affected > 0) {
        echo json_encode([
            "success" => true,
            "message" => "Đã cập nhật phản hồi thành công"
        ]);
    } else {
        echo json_encode([
            "success" => false,
            "message" => "Không tìm thấy review để cập nhật hoặc không có thay đổi"
        ]);
    }
    
    $stmt->closeCursor();
} catch (Exception $e) {
    echo json_encode([
        "success" => false,
        "message" => "Lỗi: " . $e->getMessage()
    ]);
}
$conn = null; // Đóng kết nối
?>
```

**Lưu ý:**
- Đảm bảo dùng `UPDATE product_reviews` (KHÔNG phải `reviews`)
- Kiểm tra `rowCount()` để đảm bảo có dòng nào được update không
- Đóng kết nối đúng cách

---

### 2. **reviews_admin/get_all.php** (Kiểm tra có SELECT admin_reply)

```php
<?php
header("Content-Type: application/json");
require_once "../config/config.php";

try {
    // QUAN TRỌNG: Phải SELECT admin_reply
    $stmt = $conn->prepare("
        SELECT 
            pr.id,
            pr.product_id,
            pr.user_id,
            pr.rating,
            pr.comment,
            pr.admin_reply,  -- ← PHẢI CÓ DÒNG NÀY
            pr.created_at,
            u.name AS user_name
        FROM product_reviews pr
        LEFT JOIN users u ON pr.user_id = u.id
        ORDER BY pr.created_at DESC
    ");
    
    $stmt->execute();
    $reviews = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    echo json_encode([
        "success" => true,
        "data" => [
            "reviews_admin" => $reviews
        ]
    ]);
} catch (Exception $e) {
    echo json_encode([
        "success" => false,
        "message" => "Lỗi: " . $e->getMessage()
    ]);
}
$conn = null;
?>
```

**Lưu ý:**
- Phải SELECT `pr.admin_reply` trong query
- Đảm bảo dùng bảng `product_reviews` (KHÔNG phải `reviews`)

---

## Sửa Android để hiển thị lỗi:

### File: AdminReviewActivity.java

Thêm Toast để hiển thị thông báo và lỗi:

```java
private void replyReview(int id, String msg) {
    if (msg == null || msg.trim().isEmpty()) {
        Toast.makeText(this, "Vui lòng nhập phản hồi", Toast.LENGTH_SHORT).show();
        return;
    }
    
    api.replyReview(id, msg).enqueue(new Callback<GeneralResponse>() {
        @Override
        public void onResponse(Call<GeneralResponse> call, Response<GeneralResponse> res) {
            if (res.isSuccessful() && res.body() != null) {
                if (res.body().isSuccess()) {
                    Toast.makeText(AdminReviewActivity.this, "Phản hồi thành công!", Toast.LENGTH_SHORT).show();
                    loadData(); // Refresh lại danh sách
                } else {
                    Toast.makeText(AdminReviewActivity.this, "Lỗi: " + res.body().getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(AdminReviewActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        }
        
        @Override
        public void onFailure(Call<GeneralResponse> call, Throwable t) {
            Toast.makeText(AdminReviewActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    });
}
```

---

## Checklist để debug:

1. **Kiểm tra file PHP `reviews_admin/reply.php`:**
   - [ ] Đang dùng `UPDATE product_reviews` (KHÔNG phải `reviews`)
   - [ ] Có kiểm tra `rowCount()` để đảm bảo update thành công
   - [ ] Có error handling

2. **Kiểm tra file PHP `reviews_admin/get_all.php`:**
   - [ ] Có SELECT `pr.admin_reply`
   - [ ] Đang dùng bảng `product_reviews`

3. **Test trực tiếp trong database:**
   ```sql
   -- Kiểm tra xem có dữ liệu không
   SELECT id, admin_reply FROM product_reviews WHERE id = [ID_REVIEW];
   
   -- Test update thủ công
   UPDATE product_reviews SET admin_reply = 'Test reply' WHERE id = [ID_REVIEW];
   
   -- Kiểm tra lại
   SELECT id, admin_reply FROM product_reviews WHERE id = [ID_REVIEW];
   ```

4. **Test API trực tiếp:**
   - Dùng Postman hoặc curl để test API `reviews_admin/reply.php`
   - Kiểm tra response có `success: true` không

---

## Debug steps:

1. **Kiểm tra log PHP:**
   - Bật error logging trong PHP
   - Xem có lỗi gì không

2. **Kiểm tra network request:**
   - Dùng Android Studio Network Profiler
   - Xem request có gửi đúng không
   - Xem response trả về gì

3. **Kiểm tra database:**
   - Xem trực tiếp trong phpMyAdmin
   - Kiểm tra xem `admin_reply` có được update không

---

## Code đầy đủ đã được cập nhật trong `PHP_REVIEWS_CODE_FULL.md`


