# Sửa lỗi: Không thể đặt hàng sau khi sửa code

## Các lỗi có thể xảy ra:

### 1. **Lỗi: Cột `price` không tồn tại trong bảng `order_items`**

**Triệu chứng:** Lỗi SQL khi INSERT vào order_items

**Giải pháp:**
```sql
-- Kiểm tra cấu trúc bảng
DESCRIBE order_items;

-- Nếu chưa có cột price, thêm vào:
ALTER TABLE order_items ADD COLUMN price DECIMAL(10, 2) NOT NULL DEFAULT 0;
```

---

### 2. **Lỗi: Cột `price` không tồn tại trong bảng `cart`**

**Triệu chứng:** Lỗi SQL khi SELECT từ cart

**Giải pháp:**
```sql
-- Kiểm tra cấu trúc bảng
DESCRIBE cart;

-- Nếu chưa có cột price, thêm vào:
ALTER TABLE cart ADD COLUMN price DECIMAL(10, 2) NOT NULL DEFAULT 0;

-- Cập nhật giá cho các item đã có (lấy từ products)
UPDATE cart c
JOIN products p ON c.product_id = p.id
SET c.price = p.price
WHERE c.price = 0 OR c.price IS NULL;
```

---

### 3. **Lỗi: Transaction không được hỗ trợ**

**Triệu chứng:** Lỗi khi gọi `beginTransaction()`

**Giải pháp:** Sửa code để không dùng transaction (nếu PDO không hỗ trợ):

```php
// Thay vì:
$conn->beginTransaction();
// ... code ...
$conn->commit();

// Dùng:
try {
    // ... code trực tiếp ...
} catch (PDOException $e) {
    // Xử lý lỗi
}
```

---

## Code PHP đã sửa lỗi (An toàn hơn):

### File: orders/place_order.php (Version 2 - An toàn)

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
    // Kiểm tra xem bảng cart có cột price không
    // Nếu không có, sẽ dùng giá từ products
    
    // 1. Lấy tất cả items từ cart
    // Thử lấy price từ cart trước, nếu không có thì lấy từ products
    $stmt = $conn->prepare("
        SELECT 
            c.product_id,
            c.quantity,
            COALESCE(c.price, p.price) AS price,  -- Dùng giá từ cart, nếu null thì dùng từ products
            p.name AS product_name,
            p.image
        FROM cart c
        JOIN products p ON c.product_id = p.id
        WHERE c.user_id = ?
    ");
    $stmt->execute([$user_id]);
    $cartItems = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    if (empty($cartItems)) {
        echo json_encode([
            "success" => false,
            "message" => "Giỏ hàng trống"
        ]);
        exit;
    }
    
    // 2. Tính tổng tiền
    $total = 0;
    foreach ($cartItems as $item) {
        $total += (float)$item["price"] * (int)$item["quantity"];
    }
    
    // 3. Tạo đơn hàng
    $stmt = $conn->prepare("
        INSERT INTO orders (user_id, address, phone, note, total, status, created_at) 
        VALUES (?, ?, ?, ?, ?, 'pending', NOW())
    ");
    $stmt->execute([$user_id, $address, $phone, $note, $total]);
    $orderId = $conn->lastInsertId();
    
    // 4. Tạo order_items
    // Kiểm tra xem bảng order_items có cột price không
    try {
        // Thử INSERT với price
        $stmt = $conn->prepare("
            INSERT INTO order_items (order_id, product_id, product_name, image, quantity, price) 
            VALUES (?, ?, ?, ?, ?, ?)
        ");
        
        foreach ($cartItems as $item) {
            $stmt->execute([
                $orderId,
                $item["product_id"],
                $item["product_name"],
                $item["image"],
                $item["quantity"],
                $item["price"]
            ]);
        }
    } catch (PDOException $e) {
        // Nếu lỗi do không có cột price, INSERT không có price
        if (strpos($e->getMessage(), "price") !== false || strpos($e->getMessage(), "Unknown column") !== false) {
            $stmt = $conn->prepare("
                INSERT INTO order_items (order_id, product_id, product_name, image, quantity) 
                VALUES (?, ?, ?, ?, ?)
            ");
            
            foreach ($cartItems as $item) {
                $stmt->execute([
                    $orderId,
                    $item["product_id"],
                    $item["product_name"],
                    $item["image"],
                    $item["quantity"]
                ]);
            }
        } else {
            throw $e; // Ném lại lỗi khác
        }
    }
    
    // 5. Xóa giỏ hàng
    $stmt = $conn->prepare("DELETE FROM cart WHERE user_id = ?");
    $stmt->execute([$user_id]);
    
    echo json_encode([
        "success" => true,
        "message" => "Đặt hàng thành công",
        "order_id" => $orderId
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

**Thay đổi:**
- ✅ Dùng `COALESCE(c.price, p.price)` để fallback về giá products nếu cart không có price
- ✅ Try-catch khi INSERT order_items để xử lý nếu không có cột price
- ✅ Bỏ transaction để tránh lỗi nếu không hỗ trợ

---

## Code đơn giản hơn (Không dùng transaction):

### File: orders/place_order.php (Version 3 - Đơn giản nhất)

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
    // 1. Lấy items từ cart (dùng COALESCE để fallback)
    $stmt = $conn->prepare("
        SELECT 
            c.product_id,
            c.quantity,
            COALESCE(c.price, p.price) AS price,
            p.name AS product_name,
            p.image
        FROM cart c
        JOIN products p ON c.product_id = p.id
        WHERE c.user_id = ?
    ");
    $stmt->execute([$user_id]);
    $cartItems = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    if (empty($cartItems)) {
        echo json_encode([
            "success" => false,
            "message" => "Giỏ hàng trống"
        ]);
        exit;
    }
    
    // 2. Tính tổng tiền
    $total = 0;
    foreach ($cartItems as $item) {
        $total += (float)$item["price"] * (int)$item["quantity"];
    }
    
    // 3. Tạo đơn hàng
    $stmt = $conn->prepare("
        INSERT INTO orders (user_id, address, phone, note, total, status, created_at) 
        VALUES (?, ?, ?, ?, ?, 'pending', NOW())
    ");
    $stmt->execute([$user_id, $address, $phone, $note, $total]);
    $orderId = $conn->lastInsertId();
    
    // 4. Tạo order_items (kiểm tra xem có cột price không)
    // Thử INSERT với price trước
    $hasPriceColumn = true;
    try {
        $testStmt = $conn->prepare("SELECT price FROM order_items LIMIT 1");
        $testStmt->execute();
    } catch (PDOException $e) {
        $hasPriceColumn = false;
    }
    
    if ($hasPriceColumn) {
        // Có cột price
        $stmt = $conn->prepare("
            INSERT INTO order_items (order_id, product_id, product_name, image, quantity, price) 
            VALUES (?, ?, ?, ?, ?, ?)
        ");
        foreach ($cartItems as $item) {
            $stmt->execute([
                $orderId,
                $item["product_id"],
                $item["product_name"],
                $item["image"],
                $item["quantity"],
                $item["price"]
            ]);
        }
    } else {
        // Không có cột price
        $stmt = $conn->prepare("
            INSERT INTO order_items (order_id, product_id, product_name, image, quantity) 
            VALUES (?, ?, ?, ?, ?)
        ");
        foreach ($cartItems as $item) {
            $stmt->execute([
                $orderId,
                $item["product_id"],
                $item["product_name"],
                $item["image"],
                $item["quantity"]
            ]);
        }
    }
    
    // 5. Xóa giỏ hàng
    $stmt = $conn->prepare("DELETE FROM cart WHERE user_id = ?");
    $stmt->execute([$user_id]);
    
    echo json_encode([
        "success" => true,
        "message" => "Đặt hàng thành công",
        "order_id" => $orderId
    ]);
    
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

## Cách debug:

1. **Bật error reporting trong PHP:**
   ```php
   error_reporting(E_ALL);
   ini_set('display_errors', 1);
   ```

2. **Kiểm tra log PHP:**
   - Xem file error log của PHP
   - Hoặc thêm `error_log()` vào code

3. **Test từng bước:**
   - Test query SELECT từ cart
   - Test INSERT vào orders
   - Test INSERT vào order_items

4. **Kiểm tra cấu trúc bảng:**
   ```sql
   DESCRIBE cart;
   DESCRIBE order_items;
   DESCRIBE orders;
   ```

---

## Checklist để fix:

- [ ] Kiểm tra bảng `cart` có cột `price` chưa
- [ ] Kiểm tra bảng `order_items` có cột `price` chưa
- [ ] Nếu chưa có, thêm cột hoặc dùng code fallback
- [ ] Test lại đặt hàng
- [ ] Kiểm tra log lỗi PHP


