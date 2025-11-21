# Code PHP đầy đủ - Reviews Admin (Đã sửa)

## File 1: reviews_admin/get_all.php

```php
<?php
header("Content-Type: application/json");
require_once "../config/config.php";

try {
    // Lấy tất cả reviews từ bảng product_reviews
    $stmt = $conn->prepare("
        SELECT 
            r.id,
            r.product_id,
            r.user_id,
            r.rating,
            r.comment,
            r.admin_reply,
            r.created_at,
            u.name AS user_name
        FROM product_reviews r
        LEFT JOIN users u ON r.user_id = u.id
        ORDER BY r.created_at DESC
    ");
    
    $stmt->execute();
    $reviews = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    echo json_encode([
        "success" => true,
        "data" => [
            "reviews_admin" => $reviews
        ]
    ]);
} catch (PDOException $e) {
    echo json_encode([
        "success" => false,
        "message" => "Lỗi database: " . $e->getMessage()
    ]);
} finally {
    $conn = null; // Đóng kết nối
}
?>
```

**Lưu ý:**
- ✅ Dùng `LEFT JOIN` để đảm bảo lấy được cả review không có user
- ✅ SELECT đầy đủ các trường bao gồm `admin_reply`
- ✅ Đóng kết nối trong `finally` block

---

## File 2: reviews_admin/reply.php (QUAN TRỌNG - Đã sửa)

```php
<?php
header("Content-Type: application/json");
require_once "../config/config.php";

// QUAN TRỌNG: Android gửi FormUrlEncoded, nên dùng $_POST (KHÔNG phải json_decode)
$id = $_POST["id"] ?? 0;
$reply = $_POST["reply"] ?? "";

if ($id == 0) {
    echo json_encode([
        "success" => false,
        "message" => "ID không hợp lệ"
    ]);
    exit;
}

// Validation: Phản hồi phải có ít nhất 2 ký tự
$reply = trim($reply);
if (empty($reply) || strlen($reply) < 2) {
    echo json_encode([
        "success" => false,
        "message" => "Phản hồi phải có ít nhất 2 ký tự"
    ]);
    exit;
}

try {
    // Update admin_reply trong bảng product_reviews
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
            "message" => "Gửi phản hồi thành công"
        ]);
    } else {
        echo json_encode([
            "success" => false,
            "message" => "Không tìm thấy review để cập nhật. ID: " . $id
        ]);
    }
    
    $stmt->closeCursor();
} catch (PDOException $e) {
    echo json_encode([
        "success" => false,
        "message" => "Lỗi: " . $e->getMessage()
    ]);
} finally {
    $conn = null; // Đóng kết nối
}
?>
```

**Thay đổi QUAN TRỌNG:**
- ❌ **SAI:** `json_decode(file_get_contents("php://input"), true)` 
- ✅ **ĐÚNG:** `$_POST["id"]` và `$_POST["reply"]`
- ✅ Thêm validation cho `$reply` không được rỗng
- ✅ Kiểm tra `rowCount()` để đảm bảo update thành công
- ✅ Đóng kết nối trong `finally` block

**Lý do:** 
- Android code sử dụng `@FormUrlEncoded` và `@Field`, nghĩa là dữ liệu được gửi dưới dạng form data (application/x-www-form-urlencoded), không phải JSON.
- Xem trong `APIService.java`:
  ```java
  @FormUrlEncoded
  @POST("reviews_admin/reply.php")
  Call<GeneralResponse> replyReview(
      @Field("id") int id,
      @Field("reply") String reply
  );
  ```

---

## File 3: reviews_admin/delete.php

```php
<?php
header("Content-Type: application/json");
require_once "../config/config.php";

$id = $_GET["id"] ?? 0;

if ($id == 0) {
    echo json_encode([
        "success" => false,
        "message" => "ID không hợp lệ"
    ]);
    exit;
}

try {
    // Xóa từ bảng product_reviews
    $stmt = $conn->prepare("DELETE FROM product_reviews WHERE id = ?");
    $ok = $stmt->execute([$id]);
    $affected = $stmt->rowCount();
    
    if ($ok && $affected > 0) {
        echo json_encode([
            "success" => true,
            "message" => "Đã xóa đánh giá thành công"
        ]);
    } else {
        echo json_encode([
            "success" => false,
            "message" => "Không tìm thấy review để xóa"
        ]);
    }
    
    $stmt->closeCursor();
} catch (PDOException $e) {
    echo json_encode([
        "success" => false,
        "message" => "Lỗi: " . $e->getMessage()
    ]);
} finally {
    $conn = null; // Đóng kết nối
}
?>
```

---

## File 4: reviews/get_reviews.php (User xem reviews)

```php
<?php
header("Content-Type: application/json");
require_once "../config/config.php";

$product_id = $_GET["product_id"] ?? 0;

if ($product_id == 0) {
    echo json_encode([
        "success" => false,
        "message" => "Product ID không hợp lệ"
    ]);
    exit;
}

try {
    // Lấy danh sách reviews từ bảng product_reviews (QUAN TRỌNG: có admin_reply)
    $stmt = $conn->prepare("
        SELECT 
            pr.id, 
            pr.rating, 
            pr.comment, 
            pr.created_at,
            pr.admin_reply,  -- QUAN TRỌNG: Phải có để user thấy phản hồi admin
            u.name AS user_name 
        FROM product_reviews pr
        LEFT JOIN users u ON pr.user_id = u.id
        WHERE pr.product_id = ?
        ORDER BY pr.created_at DESC
    ");
    $stmt->execute([$product_id]);
    $reviews = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Tính trung bình rating từ bảng product_reviews
    $stmt2 = $conn->prepare("
        SELECT 
            AVG(rating) AS avg_rating, 
            COUNT(*) AS total 
        FROM product_reviews 
        WHERE product_id = ?
    ");
    $stmt2->execute([$product_id]);
    $summary = $stmt2->fetch(PDO::FETCH_ASSOC);
    
    // Làm tròn avg_rating về 1 chữ số thập phân
    $summary['avg_rating'] = round((float)$summary['avg_rating'], 1);
    $summary['total'] = (int)$summary['total'];
    
    echo json_encode([
        "success" => true,
        "data" => [
            "reviews" => $reviews,
            "summary" => $summary
        ]
    ]);
    
    $stmt->closeCursor();
    $stmt2->closeCursor();
} catch (PDOException $e) {
    echo json_encode([
        "success" => false,
        "message" => "Lỗi: " . $e->getMessage()
    ]);
} finally {
    $conn = null; // Đóng kết nối
}
?>
```

---

## File 5: reviews/add_review.php (User tạo review)

```php
<?php
header("Content-Type: application/json");
require_once "../config/config.php";

$data = json_decode(file_get_contents("php://input"), true);

$product_id = $data["product_id"] ?? 0;
$user_id    = $data["user_id"] ?? 0;
$rating     = $data["rating"] ?? 0;
$comment    = $data["comment"] ?? "";

if ($product_id == 0 || $user_id == 0 || $rating == 0) {
    echo json_encode([
        "success" => false, 
        "message" => "Thiếu dữ liệu"
    ]);
    exit;
}

try {
    // Insert vào bảng product_reviews (admin_reply mặc định là NULL)
    $stmt = $conn->prepare("
        INSERT INTO product_reviews (product_id, user_id, rating, comment, created_at) 
        VALUES (?, ?, ?, ?, NOW())
    ");
    
    $ok = $stmt->execute([$product_id, $user_id, $rating, $comment]);
    
    if ($ok) {
        echo json_encode([
            "success" => true,
            "message" => "Đánh giá thành công!"
        ]);
    } else {
        echo json_encode([
            "success" => false,
            "message" => "Lỗi hệ thống!"
        ]);
    }
    
    $stmt->closeCursor();
} catch (PDOException $e) {
    echo json_encode([
        "success" => false,
        "message" => "Lỗi: " . $e->getMessage()
    ]);
} finally {
    $conn = null; // Đóng kết nối
}
?>
```

---

## Tóm tắt các thay đổi:

| File | Vấn đề | Giải pháp |
|------|--------|-----------|
| `reviews_admin/reply.php` | ❌ Dùng `json_decode` | ✅ Dùng `$_POST` (vì Android gửi FormUrlEncoded) |
| `reviews_admin/get_all.php` | ✅ Đã đúng | Thêm `LEFT JOIN` và đóng kết nối |
| `reviews/get_reviews.php` | ✅ Đã có `admin_reply` | Đảm bảo SELECT `admin_reply` |
| `reviews/add_review.php` | ✅ Đã đúng | - |
| `reviews_admin/delete.php` | ✅ Đã đúng | Thêm kiểm tra `rowCount()` |

---

## Lưu ý quan trọng:

1. **reviews_admin/reply.php:**
   - ⚠️ **PHẢI** dùng `$_POST` thay vì `json_decode` vì Android gửi FormUrlEncoded
   - ✅ Kiểm tra `rowCount()` để đảm bảo update thành công
   - ✅ Validation cho `$reply` không được rỗng

2. **Tất cả các file:**
   - ✅ Đóng kết nối trong `finally` block
   - ✅ Dùng `PDOException` để bắt lỗi
   - ✅ Đảm bảo dùng bảng `product_reviews` (KHÔNG phải `reviews`)

3. **Test:**
   - Sau khi sửa, test lại chức năng admin reply
   - Kiểm tra trong database xem `admin_reply` có được update không
   - Kiểm tra user có thấy phản hồi admin không

---

## Cấu trúc bảng product_reviews:

```sql
CREATE TABLE product_reviews (
    id INT PRIMARY KEY AUTO_INCREMENT,
    product_id INT NOT NULL,
    user_id INT NOT NULL,
    rating INT NOT NULL,
    comment TEXT,
    admin_reply TEXT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

