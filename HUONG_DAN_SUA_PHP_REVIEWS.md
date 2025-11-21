# Hướng dẫn sửa PHP để đồng bộ reviews về bảng product_reviews

## Vấn đề hiện tại:
- **User** tạo review → lưu vào bảng `reviews`
- **Admin** quản lý review → đọc từ bảng `product_reviews`
- → Không đồng bộ, dữ liệu bị tách rời

## Giải pháp:
Sửa tất cả các file PHP để **cùng sử dụng bảng `product_reviews`**

---

## Các file PHP cần sửa:

### 1. **reviews/add_review.php** (User tạo review)

**Vị trí:** `backend/reviews/add_review.php`

**Cần sửa:**
- Thay đổi tên bảng từ `reviews` → `product_reviews`
- Đảm bảo có cột `admin_reply` (có thể NULL)

**Code mẫu:**

```php
<?php
header('Content-Type: application/json');
require_once '../config/database.php';

$data = json_decode(file_get_contents('php://input'), true);

if (!isset($data['product_id']) || !isset($data['user_id']) || !isset($data['rating']) || !isset($data['comment'])) {
    echo json_encode(['success' => false, 'message' => 'Thiếu thông tin']);
    exit;
}

$product_id = $data['product_id'];
$user_id = $data['user_id'];
$rating = $data['rating'];
$comment = $data['comment'];

try {
    // SỬA Ở ĐÂY: Thay 'reviews' thành 'product_reviews'
    $stmt = $conn->prepare("INSERT INTO product_reviews (product_id, user_id, rating, comment, created_at) VALUES (?, ?, ?, ?, NOW())");
    $stmt->bind_param("iiis", $product_id, $user_id, $rating, $comment);
    
    if ($stmt->execute()) {
        echo json_encode(['success' => true, 'message' => 'Đánh giá đã được thêm']);
    } else {
        echo json_encode(['success' => false, 'message' => 'Lỗi khi thêm đánh giá']);
    }
    $stmt->close();
} catch (Exception $e) {
    echo json_encode(['success' => false, 'message' => 'Lỗi: ' . $e->getMessage()]);
}
$conn->close();
?>
```

**Thay đổi chính:**
```php
// TRƯỚC:
INSERT INTO reviews (product_id, user_id, rating, comment, created_at) ...

// SAU:
INSERT INTO product_reviews (product_id, user_id, rating, comment, created_at, admin_reply) ...
// Hoặc nếu admin_reply có giá trị mặc định NULL thì không cần thêm vào INSERT
```

---

### 2. **reviews/get_reviews.php** (User xem danh sách reviews)

**Vị trí:** `backend/reviews/get_reviews.php`

**Cần sửa:**
- Thay đổi tên bảng từ `reviews` → `product_reviews`
- JOIN với bảng `users` để lấy `user_name`
- Tính toán `avg_rating` và `total` từ bảng `product_reviews`

**Code mẫu:**

```php
<?php
header('Content-Type: application/json');
require_once '../config/database.php';

$product_id = isset($_GET['product_id']) ? (int)$_GET['product_id'] : 0;

if ($product_id <= 0) {
    echo json_encode(['success' => false, 'message' => 'Product ID không hợp lệ']);
    exit;
}

try {
    // SỬA Ở ĐÂY: Thay 'reviews' thành 'product_reviews'
    // Lấy danh sách reviews
    $stmt = $conn->prepare("
        SELECT 
            pr.id,
            pr.rating,
            pr.comment,
            pr.created_at,
            u.name as user_name
        FROM product_reviews pr
        LEFT JOIN users u ON pr.user_id = u.id
        WHERE pr.product_id = ?
        ORDER BY pr.created_at DESC
    ");
    $stmt->bind_param("i", $product_id);
    $stmt->execute();
    $result = $stmt->get_result();
    
    $reviews = [];
    while ($row = $result->fetch_assoc()) {
        $reviews[] = [
            'id' => $row['id'],
            'rating' => $row['rating'],
            'comment' => $row['comment'],
            'created_at' => $row['created_at'],
            'user_name' => $row['user_name']
        ];
    }
    $stmt->close();
    
    // Tính toán trung bình rating và tổng số reviews
    $stmt = $conn->prepare("
        SELECT 
            AVG(rating) as avg_rating,
            COUNT(*) as total
        FROM product_reviews
        WHERE product_id = ?
    ");
    $stmt->bind_param("i", $product_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $summary = $result->fetch_assoc();
    $stmt->close();
    
    echo json_encode([
        'success' => true,
        'data' => [
            'reviews' => $reviews,
            'summary' => [
                'avg_rating' => round((float)$summary['avg_rating'], 1),
                'total' => (int)$summary['total']
            ]
        ]
    ]);
} catch (Exception $e) {
    echo json_encode(['success' => false, 'message' => 'Lỗi: ' . $e->getMessage()]);
}
$conn->close();
?>
```

**Thay đổi chính:**
```php
// TRƯỚC:
FROM reviews pr
...
FROM reviews

// SAU:
FROM product_reviews pr
...
FROM product_reviews
```

---

### 3. **reviews_admin/get_all.php** (Admin xem tất cả reviews)

**Vị trí:** `backend/reviews_admin/get_all.php`

**Kiểm tra:** File này có thể đã đúng (đang dùng `product_reviews`), nhưng cần đảm bảo:

```php
<?php
header('Content-Type: application/json');
require_once '../config/database.php';

try {
    // Đảm bảo đang dùng bảng product_reviews
    $stmt = $conn->prepare("
        SELECT 
            pr.id,
            pr.product_id,
            pr.user_id,
            pr.rating,
            pr.comment,
            pr.admin_reply,
            pr.created_at,
            u.name as user_name
        FROM product_reviews pr
        LEFT JOIN users u ON pr.user_id = u.id
        ORDER BY pr.created_at DESC
    ");
    $stmt->execute();
    $result = $stmt->get_result();
    
    $reviews = [];
    while ($row = $result->fetch_assoc()) {
        $reviews[] = [
            'id' => $row['id'],
            'product_id' => $row['product_id'],
            'user_id' => $row['user_id'],
            'rating' => $row['rating'],
            'comment' => $row['comment'],
            'admin_reply' => $row['admin_reply'],
            'user_name' => $row['user_name']
        ];
    }
    $stmt->close();
    
    echo json_encode([
        'success' => true,
        'data' => [
            'reviews_admin' => $reviews
        ]
    ]);
} catch (Exception $e) {
    echo json_encode(['success' => false, 'message' => 'Lỗi: ' . $e->getMessage()]);
}
$conn->close();
?>
```

---

### 4. **reviews_admin/delete.php** (Admin xóa review)

**Vị trí:** `backend/reviews_admin/delete.php`

**Kiểm tra:** Đảm bảo đang xóa từ bảng `product_reviews`:

```php
<?php
header('Content-Type: application/json');
require_once '../config/database.php';

$id = isset($_GET['id']) ? (int)$_GET['id'] : 0;

if ($id <= 0) {
    echo json_encode(['success' => false, 'message' => 'ID không hợp lệ']);
    exit;
}

try {
    // Đảm bảo đang xóa từ product_reviews
    $stmt = $conn->prepare("DELETE FROM product_reviews WHERE id = ?");
    $stmt->bind_param("i", $id);
    
    if ($stmt->execute()) {
        echo json_encode(['success' => true, 'message' => 'Đã xóa đánh giá']);
    } else {
        echo json_encode(['success' => false, 'message' => 'Lỗi khi xóa']);
    }
    $stmt->close();
} catch (Exception $e) {
    echo json_encode(['success' => false, 'message' => 'Lỗi: ' . $e->getMessage()]);
}
$conn->close();
?>
```

---

### 5. **reviews_admin/reply.php** (Admin trả lời review)

**Vị trí:** `backend/reviews_admin/reply.php`

**Kiểm tra:** Đảm bảo đang update bảng `product_reviews`:

```php
<?php
header('Content-Type: application/json');
require_once '../config/database.php';

$id = isset($_POST['id']) ? (int)$_POST['id'] : 0;
$reply = isset($_POST['reply']) ? trim($_POST['reply']) : '';

if ($id <= 0) {
    echo json_encode(['success' => false, 'message' => 'ID không hợp lệ']);
    exit;
}

try {
    // Đảm bảo đang update bảng product_reviews
    $stmt = $conn->prepare("UPDATE product_reviews SET admin_reply = ? WHERE id = ?");
    $stmt->bind_param("si", $reply, $id);
    
    if ($stmt->execute()) {
        echo json_encode(['success' => true, 'message' => 'Đã cập nhật phản hồi']);
    } else {
        echo json_encode(['success' => false, 'message' => 'Lỗi khi cập nhật']);
    }
    $stmt->close();
} catch (Exception $e) {
    echo json_encode(['success' => false, 'message' => 'Lỗi: ' . $e->getMessage()]);
}
$conn->close();
?>
```

---

## Tóm tắt các thay đổi:

| File PHP | Thay đổi chính |
|----------|----------------|
| `reviews/add_review.php` | `INSERT INTO reviews` → `INSERT INTO product_reviews` |
| `reviews/get_reviews.php` | `FROM reviews` → `FROM product_reviews` |
| `reviews_admin/get_all.php` | Kiểm tra đang dùng `product_reviews` |
| `reviews_admin/delete.php` | Kiểm tra `DELETE FROM product_reviews` |
| `reviews_admin/reply.php` | Kiểm tra `UPDATE product_reviews` |

---

## Lưu ý quan trọng:

1. **Backup dữ liệu** trước khi sửa
2. **Kiểm tra cấu trúc bảng `product_reviews`** có đủ các cột:
   - `id` (INT, PRIMARY KEY, AUTO_INCREMENT)
   - `product_id` (INT)
   - `user_id` (INT)
   - `rating` (INT)
   - `comment` (TEXT)
   - `admin_reply` (TEXT, NULL)
   - `created_at` (DATETIME hoặc TIMESTAMP)

3. **Nếu bảng `reviews` có dữ liệu cũ**, có thể migrate dữ liệu:
   ```sql
   INSERT INTO product_reviews (product_id, user_id, rating, comment, created_at)
   SELECT product_id, user_id, rating, comment, created_at
   FROM reviews
   WHERE id NOT IN (SELECT id FROM product_reviews);
   ```

4. **Sau khi sửa**, test lại:
   - User tạo review → kiểm tra có lưu vào `product_reviews` không
   - User xem reviews → kiểm tra có hiển thị đúng không
   - Admin xem reviews → kiểm tra có thấy review mới của user không
   - Admin reply → kiểm tra có update được không

---

## Sau khi sửa xong:

Có thể xóa hoặc giữ lại bảng `reviews` cũ (không dùng nữa) tùy theo nhu cầu.


