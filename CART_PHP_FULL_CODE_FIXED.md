# Code PHP đầy đủ - Cart (Đã sửa lỗi giá phiên bản)

## ⚠️ QUAN TRỌNG: Kiểm tra cấu trúc bảng cart

Trước khi sửa code, đảm bảo bảng `cart` có cột `price`:

```sql
-- Kiểm tra cấu trúc bảng
DESCRIBE cart;

-- Nếu chưa có cột price, thêm vào:
ALTER TABLE cart ADD COLUMN price DECIMAL(10, 2) NOT NULL DEFAULT 0;
```

---

## File 1: cart/add.php (Đã sửa đầy đủ)

```php
<?php
header("Content-Type: application/json");
require_once "../config/config.php";

// Lấy dữ liệu JSON từ client
$data = json_decode(file_get_contents("php://input"), true);

$user_id    = $data["user_id"] ?? null;
$product_id = $data["product_id"] ?? null;
$quantity   = $data["quantity"] ?? 1;
$price      = $data["price"] ?? 0; // QUAN TRỌNG: Lấy giá từ request

if (!$user_id || !$product_id || $quantity <= 0) {
    echo json_encode([
        "success" => false,
        "message" => "Thiếu thông tin người dùng, sản phẩm hoặc số lượng không hợp lệ."
    ]);
    exit;
}

// Nếu không có price trong request, lấy từ products (fallback)
if ($price == 0) {
    $stmt = $conn->prepare("SELECT price FROM products WHERE id = ?");
    $stmt->execute([$product_id]);
    $product = $stmt->fetch(PDO::FETCH_ASSOC);
    $price = $product ? (float)$product["price"] : 0;
}

try {
    // Kiểm tra xem sản phẩm đã có trong giỏ hàng chưa
    $stmt = $conn->prepare("SELECT id, quantity, price FROM cart WHERE user_id = ? AND product_id = ?");
    $stmt->execute([$user_id, $product_id]);
    $cartItem = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if ($cartItem) {
        // Nếu có rồi → cập nhật số lượng và giá (nếu giá khác)
        $newQuantity = $cartItem["quantity"] + $quantity;
        
        // QUAN TRỌNG: Cập nhật cả quantity và price
        $updateStmt = $conn->prepare("UPDATE cart SET quantity = ?, price = ? WHERE id = ?");
        $updateStmt->execute([$newQuantity, $price, $cartItem["id"]]);
    } else {
        // Nếu chưa có → thêm mới với giá từ request
        $insertStmt = $conn->prepare("INSERT INTO cart (user_id, product_id, quantity, price) VALUES (?, ?, ?, ?)");
        $insertStmt->execute([$user_id, $product_id, $quantity, $price]);
    }
    
    echo json_encode([
        "success" => true,
        "message" => "Đã thêm vào giỏ hàng thành công."
    ]);
    
    $stmt->closeCursor();
    if (isset($updateStmt)) $updateStmt->closeCursor();
    if (isset($insertStmt)) $insertStmt->closeCursor();
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
- ✅ Thêm: `$price = $data["price"] ?? 0;`
- ✅ INSERT: Thêm `price` vào VALUES
- ✅ UPDATE: Cập nhật cả `quantity` và `price`

---

## File 2: cart/view.php (Đã sửa đầy đủ)

```php
<?php
header("Content-Type: application/json");
require_once "../config/config.php";

// Nhận user_id từ client
$user_id = $_GET["user_id"] ?? null;

if (!$user_id) {
    echo json_encode([
        "success" => false,
        "message" => "Thiếu ID người dùng."
    ]);
    exit;
}

try {
    // QUAN TRỌNG: SELECT price từ cart (KHÔNG phải từ products)
    $stmt = $conn->prepare("
        SELECT 
            c.id AS cart_id,
            c.quantity,
            c.price,  -- QUAN TRỌNG: Giá từ cart (đã tính theo phiên bản)
            p.id AS product_id,
            p.name AS product_name,
            p.price AS base_price,  -- Giá gốc từ products (để tham khảo)
            p.image
        FROM cart c
        JOIN products p ON c.product_id = p.id
        WHERE c.user_id = ?
        ORDER BY c.id DESC
    ");
    
    $stmt->execute([$user_id]);
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    $result = [];
    foreach ($rows as $row) {
        $result[] = [
            "id" => (int)$row["cart_id"],
            "quantity" => (int)$row["quantity"],
            "price" => (float)$row["price"],  // QUAN TRỌNG: Giá từ cart
            "product" => [
                "id" => (int)$row["product_id"],
                "name" => $row["product_name"],
                "price" => (float)$row["base_price"],  // Giá gốc
                "image" => $row["image"]
            ]
        ];
    }
    
    echo json_encode([
        "success" => true,
        "cart_items" => $result  // Đổi tên từ "cart" thành "cart_items" để khớp với Android
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

**Thay đổi:**
- ✅ SELECT `c.price` từ cart (KHÔNG phải `p.price`)
- ✅ Trả về `price` từ cart trong response
- ✅ Thêm `id` của cart item
- ✅ Đổi key từ `"cart"` thành `"cart_items"` để khớp với Android model

---

## File 3: cart/update.php (Đã sửa đầy đủ)

```php
<?php
header("Content-Type: application/json");
require_once "../config/config.php";

// Nhận dữ liệu từ client (POST dạng JSON)
$data = json_decode(file_get_contents("php://input"), true);

$user_id    = $data["user_id"] ?? null;
$product_id = $data["product_id"] ?? null;
$quantity   = $data["quantity"] ?? null;
$price       = $data["price"] ?? 0; // QUAN TRỌNG: Lấy giá từ request

if (!$user_id || !$product_id || $quantity === null) {
    echo json_encode([
        "success" => false,
        "message" => "Thiếu dữ liệu đầu vào."
    ]);
    exit;
}

try {
    if ($quantity <= 0) {
        // Nếu số lượng = 0 thì xoá khỏi giỏ hàng
        $stmt = $conn->prepare("DELETE FROM cart WHERE user_id = ? AND product_id = ?");
        $stmt->execute([$user_id, $product_id]);
        
        echo json_encode([
            "success" => true,
            "message" => "Đã xoá sản phẩm khỏi giỏ hàng."
        ]);
    } else {
        // QUAN TRỌNG: Nếu không có price trong request, giữ nguyên giá cũ
        if ($price == 0) {
            $stmt = $conn->prepare("SELECT price FROM cart WHERE user_id = ? AND product_id = ?");
            $stmt->execute([$user_id, $product_id]);
            $existing = $stmt->fetch(PDO::FETCH_ASSOC);
            $price = $existing ? (float)$existing["price"] : 0;
        }
        
        // QUAN TRỌNG: Cập nhật cả quantity và price
        $stmt = $conn->prepare("UPDATE cart SET quantity = ?, price = ? WHERE user_id = ? AND product_id = ?");
        $stmt->execute([$quantity, $price, $user_id, $product_id]);
        
        echo json_encode([
            "success" => true,
            "message" => "Cập nhật giỏ hàng thành công."
        ]);
    }
    
    if (isset($stmt)) $stmt->closeCursor();
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

**Thay đổi:**
- ✅ Sửa: `$data["price"]` thay vì `$data->price` (sai cú pháp)
- ✅ Lấy `$price` từ request
- ✅ UPDATE cả `quantity` và `price`
- ✅ Nếu không có price trong request, giữ nguyên giá cũ

---

## Tóm tắt các lỗi đã sửa:

| File | Lỗi | Đã sửa |
|------|-----|--------|
| `cart/add.php` | ❌ Không lấy `price` từ request | ✅ `$price = $data["price"] ?? 0;` |
| `cart/add.php` | ❌ INSERT không có `price` | ✅ Thêm `price` vào INSERT |
| `cart/add.php` | ❌ UPDATE không cập nhật `price` | ✅ UPDATE cả `quantity` và `price` |
| `cart/view.php` | ❌ SELECT `p.price` từ products | ✅ SELECT `c.price` từ cart |
| `cart/view.php` | ❌ Không trả về `price` trong response | ✅ Trả về `price` từ cart |
| `cart/view.php` | ❌ Key `"cart"` không khớp Android | ✅ Đổi thành `"cart_items"` |
| `cart/update.php` | ❌ Dùng `$data->price` (sai) | ✅ Dùng `$data["price"]` |
| `cart/update.php` | ❌ Không update `price` | ✅ UPDATE cả `quantity` và `price` |

---

## Checklist trước khi test:

- [ ] **Kiểm tra bảng cart có cột price:**
  ```sql
  DESCRIBE cart;
  ```
  Nếu chưa có, chạy:
  ```sql
  ALTER TABLE cart ADD COLUMN price DECIMAL(10, 2) NOT NULL DEFAULT 0;
  ```

- [ ] **Cập nhật dữ liệu cũ (nếu có):**
  ```sql
  -- Cập nhật giá cho các item đã có trong cart (lấy từ products)
  UPDATE cart c
  JOIN products p ON c.product_id = p.id
  SET c.price = p.price
  WHERE c.price = 0 OR c.price IS NULL;
  ```

- [ ] **Test:**
  1. Thêm sản phẩm với phiên bản đặc biệt → kiểm tra giá trong cart
  2. Update số lượng → kiểm tra giá vẫn đúng
  3. Xem giỏ hàng → kiểm tra giá hiển thị đúng

---

## Cấu trúc bảng cart mong muốn:

```sql
CREATE TABLE cart (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    price DECIMAL(10, 2) NOT NULL DEFAULT 0,  -- QUAN TRỌNG
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES products(id),
    UNIQUE KEY unique_cart_item (user_id, product_id)
);
```


