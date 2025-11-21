# Code PHP đầy đủ - Reviews sử dụng bảng product_reviews

## File 1: reviews/get_reviews.php

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

// Lấy danh sách reviews từ bảng product_reviews (QUAN TRỌNG: Thêm admin_reply)
$stmt = $conn->prepare("
    SELECT 
        pr.id, 
        pr.rating, 
        pr.comment, 
        pr.created_at, 
        pr.admin_reply,
        u.name AS user_name 
    FROM product_reviews pr
    JOIN users u ON pr.user_id = u.id
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
?>
```

**Thay đổi:**
- `FROM reviews r` → `FROM product_reviews pr`
- `WHERE r.product_id` → `WHERE pr.product_id`
- `FROM reviews` (trong query tính toán) → `FROM product_reviews`
- Thêm validation cho `product_id`
- Làm tròn `avg_rating` và ép kiểu `total`

---

## File 2: reviews/add_review.php

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
?>
```

**Thay đổi:**
- `INSERT INTO reviews` → `INSERT INTO product_reviews`
- Thêm `created_at` với giá trị `NOW()` để tự động set thời gian
- Cải thiện thông báo lỗi

---

## File 3: reviews_admin/get_all.php (Kiểm tra và đảm bảo đúng)

```php
<?php
header("Content-Type: application/json");
require_once "../config/config.php";

try {
    // Lấy tất cả reviews từ bảng product_reviews
    $stmt = $conn->prepare("
        SELECT 
            pr.id,
            pr.product_id,
            pr.user_id,
            pr.rating,
            pr.comment,
            pr.admin_reply,
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
?>
```

---

## File 4: reviews_admin/delete.php (Kiểm tra và đảm bảo đúng)

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
    
    if ($ok) {
        echo json_encode([
            "success" => true,
            "message" => "Đã xóa đánh giá thành công"
        ]);
    } else {
        echo json_encode([
            "success" => false,
            "message" => "Lỗi khi xóa đánh giá"
        ]);
    }
} catch (Exception $e) {
    echo json_encode([
        "success" => false,
        "message" => "Lỗi: " . $e->getMessage()
    ]);
}
?>
```

---

## File 5: reviews_admin/reply.php (QUAN TRỌNG - Kiểm tra kỹ)

```php
<?php
header("Content-Type: application/json");
require_once "../config/config.php";

// Lấy dữ liệu từ POST
$id = $_POST["id"] ?? 0;
$reply = $_POST["reply"] ?? "";

if ($id == 0) {
    echo json_encode([
        "success" => false,
        "message" => "ID không hợp lệ"
    ]);
    exit;
}

try {
    // QUAN TRỌNG: Update vào bảng product_reviews (KHÔNG phải reviews)
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
            "message" => "Không tìm thấy review để cập nhật hoặc không có thay đổi. ID: " . $id
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

**Lưu ý QUAN TRỌNG:**
- ✅ Phải dùng `UPDATE product_reviews` (KHÔNG phải `reviews`)
- ✅ Kiểm tra `rowCount()` để đảm bảo có dòng nào được update
- ✅ Đóng kết nối đúng cách

---

## Tóm tắt thay đổi:

| File | Thay đổi chính |
|------|----------------|
| `reviews/get_reviews.php` | `reviews` → `product_reviews` (2 chỗ) |
| `reviews/add_review.php` | `reviews` → `product_reviews`, thêm `created_at` |
| `reviews_admin/get_all.php` | Đảm bảo dùng `product_reviews` |
| `reviews_admin/delete.php` | Đảm bảo xóa từ `product_reviews` |
| `reviews_admin/reply.php` | Đảm bảo update `product_reviews` |

---

## Cấu trúc bảng product_reviews cần có:

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

---

## Lưu ý:

1. **Backup dữ liệu** trước khi thay đổi
2. **Kiểm tra cấu trúc bảng** `product_reviews` có đủ các cột
3. **Test kỹ** sau khi sửa:
   - User tạo review → kiểm tra có lưu vào `product_reviews`
   - User xem reviews → kiểm tra hiển thị đúng
   - Admin xem reviews → kiểm tra thấy review mới
   - Admin reply → kiểm tra update được

4. **Nếu có dữ liệu cũ trong bảng `reviews`**, có thể migrate:
   ```sql
   INSERT INTO product_reviews (product_id, user_id, rating, comment, created_at)
   SELECT product_id, user_id, rating, comment, created_at
   FROM reviews
   WHERE id NOT IN (SELECT id FROM product_reviews);
   ```

