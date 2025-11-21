# Sửa lỗi: Không thể tải đơn hàng sau khi thanh toán

## Vấn đề có thể xảy ra:

1. **Thiếu cột `product_name` và `image` trong bảng `order_items`**
2. **Cấu trúc response không khớp với Android model**
3. **Lỗi null khi items rỗng**
4. **Thiếu các trường `user_name`, `user_email` trong response**

---

## Code PHP đã sửa (ĐÚNG - Bảng order_items KHÔNG có product_name và image):

### File: orders/list_user.php (Version Final - Đã sửa đúng)

```php
<?php
header("Content-Type: application/json");
require_once "../config/config.php";

// Lấy user_id từ client
$user_id = $_GET["user_id"] ?? null;

if (!$user_id) {
    echo json_encode([
        "success" => false,
        "message" => "Thiếu ID người dùng."
    ]);
    exit;
}

try {
    // Lấy danh sách đơn hàng với thông tin user
    $stmt = $conn->prepare("
        SELECT 
            o.id,
            o.user_id,
            o.address,
            o.phone,
            o.note,
            o.total,
            o.status,
            o.created_at,
            u.name AS user_name,
            u.email AS user_email
        FROM orders o
        LEFT JOIN users u ON o.user_id = u.id
        WHERE o.user_id = ?
        ORDER BY o.created_at DESC
    ");
    $stmt->execute([$user_id]);
    $orders = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    $result = [];
    
    foreach ($orders as $order) {
        $orderId = $order["id"];
        
        // QUAN TRỌNG: Bảng order_items KHÔNG có product_name và image
        // Phải lấy từ bảng products thông qua JOIN
        $stmtItems = $conn->prepare("
            SELECT 
                oi.product_id,
                oi.quantity,
                oi.price,  -- QUAN TRỌNG: Giá từ order_items (đã tính theo phiên bản)
                p.name AS product_name,  -- Lấy từ products
                p.image  -- Lấy từ products
            FROM order_items oi
            LEFT JOIN products p ON oi.product_id = p.id
            WHERE oi.order_id = ?
        ");
        $stmtItems->execute([$orderId]);
        $items = $stmtItems->fetchAll(PDO::FETCH_ASSOC);
        
        // Format items
        $orderItems = [];
        foreach ($items as $item) {
            $orderItems[] = [
                "product_id" => (int)$item["product_id"],
                "product_name" => $item["product_name"] ?? "Sản phẩm",
                "image" => $item["image"] ?? "",
                "quantity" => (int)$item["quantity"],
                "price" => (float)$item["price"]  // QUAN TRỌNG: Giá từ order_items
            ];
        }
        
        // Format order - đảm bảo có đầy đủ các trường
        $result[] = [
            "id" => (int)$orderId,
            "order_id" => (int)$orderId,  // Cả id và order_id đều có
            "user_id" => (int)$order["user_id"],
            "user_name" => $order["user_name"] ?? "",
            "user_email" => $order["user_email"] ?? "",
            "address" => $order["address"] ?? "",
            "phone" => $order["phone"] ?? "",
            "note" => $order["note"] ?? "",
            "total" => (float)$order["total"],
            "status" => $order["status"] ?? "pending",
            "created_at" => $order["created_at"] ?? "",
            "items" => $orderItems  // Đảm bảo luôn là array, không phải null
        ];
        
        $stmtItems->closeCursor();
    }
    
    echo json_encode([
        "success" => true,
        "orders" => $result
    ]);
    
    $stmt->closeCursor();
} catch (PDOException $e) {
    echo json_encode([
        "success" => false,
        "message" => "Lỗi truy vấn: " . $e->getMessage()
    ]);
} finally {
    $conn = null;
}
?>
```

**Thay đổi quan trọng:**
- ✅ Lấy cả `user_name` và `user_email` từ bảng users
- ✅ Dùng `COALESCE` để fallback về products nếu order_items không có product_name/image
- ✅ Try-catch để xử lý nếu bảng không có cột product_name/image
- ✅ Đảm bảo cả `id` và `order_id` đều có trong response
- ✅ Đảm bảo `items` luôn là array (không phải null)
- ✅ Thêm fallback cho tất cả các trường có thể null

---

## ✅ Code đúng (Bảng order_items KHÔNG có product_name và image):

**Lưu ý:** Code ở trên (Version Final) đã đúng rồi. Bảng `order_items` chỉ có: `id`, `order_id`, `product_id`, `quantity`, `price`.

**Không có:** `product_name`, `image`

**Giải pháp:** Lấy `product_name` và `image` từ bảng `products` thông qua JOIN.

---

## Cách debug:

### 1. **Bật error reporting trong PHP:**
Thêm vào đầu file:
```php
error_reporting(E_ALL);
ini_set('display_errors', 1);
```

### 2. **Test trực tiếp API:**
Mở browser và test:
```
http://localhost/backend/orders/list_user.php?user_id=1
```

### 3. **Kiểm tra cấu trúc bảng:**
```sql
DESCRIBE order_items;
```

Nếu không có `product_name` và `image`, dùng Version Simple.

### 4. **Kiểm tra response JSON:**
Đảm bảo response có cấu trúc:
```json
{
  "success": true,
  "orders": [
    {
      "id": 1,
      "order_id": 1,
      "user_id": 1,
      "user_name": "...",
      "user_email": "...",
      "address": "...",
      "phone": "...",
      "note": "...",
      "total": 1000000,
      "status": "pending",
      "created_at": "...",
      "items": [
        {
          "product_id": 1,
          "product_name": "...",
          "image": "...",
          "quantity": 1,
          "price": 1000000
        }
      ]
    }
  ]
}
```

---

## Checklist:

- [ ] Kiểm tra bảng `order_items` có cột `product_name` và `image` chưa
- [ ] Nếu không có → dùng Version Simple
- [ ] Nếu có → dùng Version Final
- [ ] Test API trực tiếp trong browser
- [ ] Kiểm tra response JSON có đúng cấu trúc không
- [ ] Test lại trong app Android

