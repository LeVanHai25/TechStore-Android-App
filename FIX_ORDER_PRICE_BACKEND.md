# Sửa lỗi: Giá trong đơn hàng không đúng (chỉ hiển thị giá cũ)

## Vấn đề:
- Khi đặt hàng, giá trong đơn hàng chỉ hiển thị giá gốc của sản phẩm
- Không tính giá đã tăng khi chọn phiên bản đặc biệt (Trắng/Xám)

## Nguyên nhân:
Backend không lấy giá từ `cart` khi tạo `order_items`, hoặc khi lấy đơn hàng không trả về giá đã lưu trong `order_items`.

---

## Cần sửa các file PHP:

### 1. **orders/place_order.php** - Lưu giá từ cart vào order_items

**Vấn đề:** Backend có thể đang lấy giá từ bảng `products` thay vì từ `cart`.

**Code cần sửa:**

```php
<?php
header("Content-Type: application/json");
require_once "../config/config.php";

$data = json_decode(file_get_contents("php://input"), true);

$user_id = $data["user_id"] ?? 0;
$address = $data["address"] ?? "";
$phone = $data["phone"] ?? "";
$note = $data["note"] ?? "";

if ($user_id == 0 || empty($address) || empty($phone)) {
    echo json_encode([
        "success" => false,
        "message" => "Thiếu thông tin"
    ]);
    exit;
}

try {
    // Bắt đầu transaction
    $conn->beginTransaction();
    
    // 1. Lấy tất cả items từ cart với giá đã tính (QUAN TRỌNG: lấy price từ cart)
    $stmt = $conn->prepare("
        SELECT 
            c.product_id,
            c.quantity,
            c.price,  -- QUAN TRỌNG: Giá từ cart (đã tính theo phiên bản)
            p.name AS product_name,
            p.image
        FROM cart c
        JOIN products p ON c.product_id = p.id
        WHERE c.user_id = ?
    ");
    $stmt->execute([$user_id]);
    $cartItems = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    if (empty($cartItems)) {
        $conn->rollBack();
        echo json_encode([
            "success" => false,
            "message" => "Giỏ hàng trống"
        ]);
        exit;
    }
    
    // 2. Tính tổng tiền từ giá trong cart
    $total = 0;
    foreach ($cartItems as $item) {
        $total += $item["price"] * $item["quantity"];
    }
    
    // 3. Tạo đơn hàng
    $stmt = $conn->prepare("
        INSERT INTO orders (user_id, address, phone, note, total, status, created_at) 
        VALUES (?, ?, ?, ?, ?, 'pending', NOW())
    ");
    $stmt->execute([$user_id, $address, $phone, $note, $total]);
    $orderId = $conn->lastInsertId();
    
    // 4. Tạo order_items với giá từ cart (QUAN TRỌNG)
    $stmt = $conn->prepare("
        INSERT INTO order_items (order_id, product_id, product_name, image, quantity, price) 
        VALUES (?, ?, ?, ?, ?, ?)
    ");
    
    foreach ($cartItems as $item) {
        // QUAN TRỌNG: Dùng price từ cart (KHÔNG phải từ products)
        $stmt->execute([
            $orderId,
            $item["product_id"],
            $item["product_name"],
            $item["image"],
            $item["quantity"],
            $item["price"]  // Giá từ cart (đã tính theo phiên bản)
        ]);
    }
    
    // 5. Xóa giỏ hàng sau khi đặt hàng thành công
    $stmt = $conn->prepare("DELETE FROM cart WHERE user_id = ?");
    $stmt->execute([$user_id]);
    
    // Commit transaction
    $conn->commit();
    
    echo json_encode([
        "success" => true,
        "message" => "Đặt hàng thành công",
        "order_id" => $orderId
    ]);
    
    $stmt->closeCursor();
} catch (PDOException $e) {
    $conn->rollBack();
    echo json_encode([
        "success" => false,
        "message" => "Lỗi: " . $e->getMessage()
    ]);
} finally {
    $conn = null;
}
?>
```

**Thay đổi quan trọng:**
- ✅ SELECT `c.price` từ cart (KHÔNG phải `p.price` từ products)
- ✅ Tính tổng tiền từ giá trong cart
- ✅ INSERT vào `order_items` với `price` từ cart
- ✅ Dùng transaction để đảm bảo tính nhất quán

---

### 2. **orders/list_user.php** - Trả về giá từ order_items

**Vấn đề:** Backend có thể đang trả về giá từ bảng `products` thay vì từ `order_items`.

**Code cần sửa:**

```php
<?php
header("Content-Type: application/json");
require_once "../config/config.php";

$user_id = $_GET["user_id"] ?? 0;

if ($user_id == 0) {
    echo json_encode([
        "success" => false,
        "message" => "User ID không hợp lệ"
    ]);
    exit;
}

try {
    // Lấy danh sách đơn hàng
    $stmt = $conn->prepare("
        SELECT 
            id,
            user_id,
            address,
            phone,
            note,
            total,
            status,
            created_at
        FROM orders
        WHERE user_id = ?
        ORDER BY created_at DESC
    ");
    $stmt->execute([$user_id]);
    $orders = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    $result = [];
    foreach ($orders as $order) {
        // Lấy order_items với giá đã lưu (QUAN TRỌNG: lấy price từ order_items)
        $stmt2 = $conn->prepare("
            SELECT 
                product_id,
                product_name,
                image,
                quantity,
                price  -- QUAN TRỌNG: Giá từ order_items (đã tính theo phiên bản)
            FROM order_items
            WHERE order_id = ?
        ");
        $stmt2->execute([$order["id"]]);
        $items = $stmt2->fetchAll(PDO::FETCH_ASSOC);
        
        $orderItems = [];
        foreach ($items as $item) {
            $orderItems[] = [
                "product_id" => (int)$item["product_id"],
                "product_name" => $item["product_name"],
                "image" => $item["image"],
                "quantity" => (int)$item["quantity"],
                "price" => (float)$item["price"]  // QUAN TRỌNG: Giá từ order_items
            ];
        }
        
        $result[] = [
            "id" => (int)$order["id"],
            "user_id" => (int)$order["user_id"],
            "address" => $order["address"],
            "phone" => $order["phone"],
            "note" => $order["note"],
            "total" => (float)$order["total"],
            "status" => $order["status"],
            "created_at" => $order["created_at"],
            "items" => $orderItems
        ];
        
        $stmt2->closeCursor();
    }
    
    echo json_encode([
        "success" => true,
        "orders" => $result
    ]);
    
    $stmt->closeCursor();
} catch (PDOException $e) {
    echo json_encode([
        "success" => false,
        "message" => "Lỗi: " . $e->getMessage()
    ]);
} finally {
    $conn = null;
}
?>
```

**Thay đổi quan trọng:**
- ✅ SELECT `price` từ `order_items` (KHÔNG phải từ `products`)
- ✅ Trả về `price` từ order_items trong response

---

### 3. **orders/getAllOrders.php** (Admin) - Trả về giá từ order_items

**Code cần sửa:**

```php
<?php
header("Content-Type: application/json");
require_once "../config/config.php";

try {
    // Lấy tất cả đơn hàng
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
            u.name AS user_name
        FROM orders o
        LEFT JOIN users u ON o.user_id = u.id
        ORDER BY o.created_at DESC
    ");
    $stmt->execute();
    $orders = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    $result = [];
    foreach ($orders as $order) {
        // Lấy order_items với giá đã lưu
        $stmt2 = $conn->prepare("
            SELECT 
                product_id,
                product_name,
                image,
                quantity,
                price  -- QUAN TRỌNG: Giá từ order_items
            FROM order_items
            WHERE order_id = ?
        ");
        $stmt2->execute([$order["id"]]);
        $items = $stmt2->fetchAll(PDO::FETCH_ASSOC);
        
        $orderItems = [];
        foreach ($items as $item) {
            $orderItems[] = [
                "product_id" => (int)$item["product_id"],
                "product_name" => $item["product_name"],
                "image" => $item["image"],
                "quantity" => (int)$item["quantity"],
                "price" => (float)$item["price"]  // Giá từ order_items
            ];
        }
        
        $result[] = [
            "id" => (int)$order["id"],
            "user_id" => (int)$order["user_id"],
            "user_name" => $order["user_name"],
            "address" => $order["address"],
            "phone" => $order["phone"],
            "note" => $order["note"],
            "total" => (float)$order["total"],
            "status" => $order["status"],
            "created_at" => $order["created_at"],
            "items" => $orderItems
        ];
        
        $stmt2->closeCursor();
    }
    
    echo json_encode([
        "success" => true,
        "orders" => $result
    ]);
    
    $stmt->closeCursor();
} catch (PDOException $e) {
    echo json_encode([
        "success" => false,
        "message" => "Lỗi: " . $e->getMessage()
    ]);
} finally {
    $conn = null;
}
?>
```

---

## Cấu trúc bảng order_items cần có:

```sql
CREATE TABLE order_items (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    image VARCHAR(255),
    quantity INT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,  -- QUAN TRỌNG: Giá đã tính theo phiên bản
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);
```

**Lưu ý:**
- Cột `price` phải có kiểu `DECIMAL(10, 2)` hoặc `DOUBLE`
- Nếu bảng chưa có cột `price`, cần thêm:
  ```sql
  ALTER TABLE order_items ADD COLUMN price DECIMAL(10, 2) NOT NULL DEFAULT 0;
  ```

---

## Tóm tắt thay đổi:

| File | Vấn đề | Giải pháp |
|------|--------|-----------|
| `orders/place_order.php` | Lấy giá từ products | SELECT `c.price` từ cart và INSERT vào order_items |
| `orders/list_user.php` | Trả về giá từ products | SELECT `price` từ order_items |
| `orders/getAllOrders.php` | Trả về giá từ products | SELECT `price` từ order_items |

---

## Checklist:

- [ ] **Kiểm tra bảng order_items có cột price:**
  ```sql
  DESCRIBE order_items;
  ```
  Nếu chưa có, chạy:
  ```sql
  ALTER TABLE order_items ADD COLUMN price DECIMAL(10, 2) NOT NULL DEFAULT 0;
  ```

- [ ] **Cập nhật dữ liệu cũ (nếu có):**
  ```sql
  -- Cập nhật giá cho các order_items đã có (lấy từ products - chỉ để tham khảo)
  UPDATE order_items oi
  JOIN products p ON oi.product_id = p.id
  SET oi.price = p.price
  WHERE oi.price = 0 OR oi.price IS NULL;
  ```
  **Lưu ý:** Dữ liệu cũ sẽ không chính xác vì không có thông tin về phiên bản đặc biệt.

- [ ] **Test:**
  1. Thêm sản phẩm với phiên bản đặc biệt vào cart
  2. Đặt hàng → kiểm tra giá trong order_items
  3. Xem đơn hàng → kiểm tra giá hiển thị đúng

---

## Sau khi sửa:

1. **Khi đặt hàng:**
   - Backend lấy giá từ cart: `c.price` (đã tính theo phiên bản)
   - Lưu vào order_items: `price = giá từ cart`
   - Tính tổng tiền từ giá trong cart

2. **Khi xem đơn hàng:**
   - Backend trả về giá từ order_items: `oi.price`
   - Android hiển thị giá đúng: `21,000,000₫` (nếu chọn Trắng)


