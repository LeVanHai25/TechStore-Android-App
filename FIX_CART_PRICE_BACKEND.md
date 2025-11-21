# Sửa lỗi: Giá trong giỏ hàng không đúng khi chọn phiên bản đặc biệt

## Vấn đề:
- User chọn phiên bản đặc biệt (Trắng/Xám) → giá hiển thị đúng
- Khi thêm vào giỏ hàng → giá trong giỏ hàng vẫn là giá cũ (không tăng)

## Nguyên nhân:
Backend không lưu giá từ request vào database, hoặc khi lấy giỏ hàng không trả về giá đã lưu.

---

## Cần sửa các file PHP:

### 1. **cart/add.php** - Lưu giá từ request

**Vấn đề:** Backend có thể đang lấy giá từ bảng `products` thay vì dùng giá từ request.

**Code cần sửa:**

```php
<?php
header("Content-Type: application/json");
require_once "../config/config.php";

$data = json_decode(file_get_contents("php://input"), true);

$user_id = $data["user_id"] ?? 0;
$product_id = $data["product_id"] ?? 0;
$quantity = $data["quantity"] ?? 1;
$price = $data["price"] ?? 0; // QUAN TRỌNG: Lấy giá từ request

if ($user_id == 0 || $product_id == 0) {
    echo json_encode([
        "success" => false,
        "message" => "Thiếu thông tin"
    ]);
    exit;
}

// Nếu không có price trong request, lấy từ products (fallback)
if ($price == 0) {
    $stmt = $conn->prepare("SELECT price FROM products WHERE id = ?");
    $stmt->execute([$product_id]);
    $product = $stmt->fetch(PDO::FETCH_ASSOC);
    $price = $product ? $product["price"] : 0;
}

try {
    // Kiểm tra xem sản phẩm đã có trong giỏ hàng chưa
    $stmt = $conn->prepare("
        SELECT id, quantity, price 
        FROM cart 
        WHERE user_id = ? AND product_id = ?
    ");
    $stmt->execute([$user_id, $product_id]);
    $existing = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if ($existing) {
        // Nếu đã có, cập nhật số lượng và giá (nếu giá khác)
        $newQuantity = $existing["quantity"] + $quantity;
        // QUAN TRỌNG: Cập nhật giá nếu giá mới khác giá cũ
        $stmt = $conn->prepare("
            UPDATE cart 
            SET quantity = ?, price = ? 
            WHERE id = ?
        ");
        $stmt->execute([$newQuantity, $price, $existing["id"]]);
    } else {
        // Nếu chưa có, thêm mới với giá từ request
        $stmt = $conn->prepare("
            INSERT INTO cart (user_id, product_id, quantity, price) 
            VALUES (?, ?, ?, ?)
        ");
        $stmt->execute([$user_id, $product_id, $quantity, $price]);
    }
    
    echo json_encode([
        "success" => true,
        "message" => "Đã thêm vào giỏ hàng"
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
- ✅ Lấy `$price` từ request: `$price = $data["price"] ?? 0;`
- ✅ Khi INSERT: dùng `$price` từ request (KHÔNG lấy từ products)
- ✅ Khi UPDATE: cập nhật cả `price` nếu giá mới khác giá cũ

---

### 2. **cart/view.php** - Trả về giá đã lưu trong cart

**Vấn đề:** Backend có thể đang trả về giá từ bảng `products` thay vì giá đã lưu trong `cart`.

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
    // QUAN TRỌNG: SELECT price từ cart (KHÔNG phải từ products)
    $stmt = $conn->prepare("
        SELECT 
            c.id,
            c.quantity,
            c.price,  -- QUAN TRỌNG: Lấy giá từ cart
            p.id AS product_id,
            p.name,
            p.image,
            p.description,
            p.price AS base_price  -- Giá gốc từ products (để tham khảo)
        FROM cart c
        JOIN products p ON c.product_id = p.id
        WHERE c.user_id = ?
        ORDER BY c.id DESC
    ");
    
    $stmt->execute([$user_id]);
    $items = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Format dữ liệu để trả về
    $cartItems = [];
    foreach ($items as $item) {
        $cartItems[] = [
            "id" => $item["id"],
            "quantity" => (int)$item["quantity"],
            "price" => (float)$item["price"],  // QUAN TRỌNG: Giá từ cart
            "product" => [
                "id" => (int)$item["product_id"],
                "name" => $item["name"],
                "image" => $item["image"],
                "description" => $item["description"],
                "price" => (float)$item["base_price"]  // Giá gốc
            ]
        ];
    }
    
    echo json_encode([
        "success" => true,
        "cart_items" => $cartItems
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
- ✅ SELECT `c.price` từ bảng `cart` (KHÔNG phải `p.price`)
- ✅ Trả về `price` từ cart trong response
- ✅ `base_price` từ products chỉ để tham khảo

---

### 3. **cart/update.php** - Cập nhật giá khi update số lượng

**Code cần sửa:**

```php
<?php
header("Content-Type: application/json");
require_once "../config/config.php";

$data = json_decode(file_get_contents("php://input"), true);

$user_id = $data["user_id"] ?? 0;
$product_id = $data["product_id"] ?? 0;
$quantity = $data["quantity"] ?? 1;
$price = $data["price"] ?? 0; // QUAN TRỌNG: Lấy giá từ request

if ($user_id == 0 || $product_id == 0) {
    echo json_encode([
        "success" => false,
        "message" => "Thiếu thông tin"
    ]);
    exit;
}

// Nếu không có price trong request, giữ nguyên giá cũ
if ($price == 0) {
    $stmt = $conn->prepare("SELECT price FROM cart WHERE user_id = ? AND product_id = ?");
    $stmt->execute([$user_id, $product_id]);
    $existing = $stmt->fetch(PDO::FETCH_ASSOC);
    $price = $existing ? $existing["price"] : 0;
}

try {
    // QUAN TRỌNG: Cập nhật cả quantity và price
    $stmt = $conn->prepare("
        UPDATE cart 
        SET quantity = ?, price = ? 
        WHERE user_id = ? AND product_id = ?
    ");
    
    $ok = $stmt->execute([$quantity, $price, $user_id, $product_id]);
    
    if ($ok) {
        echo json_encode([
            "success" => true,
            "message" => "Đã cập nhật giỏ hàng"
        ]);
    } else {
        echo json_encode([
            "success" => false,
            "message" => "Không thể cập nhật"
        ]);
    }
    
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
- ✅ Lấy `$price` từ request
- ✅ UPDATE cả `quantity` và `price`

---

## Cấu trúc bảng cart cần có:

```sql
CREATE TABLE cart (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    price DECIMAL(10, 2) NOT NULL,  -- QUAN TRỌNG: Giá đã tính theo phiên bản
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES products(id),
    UNIQUE KEY unique_cart_item (user_id, product_id)
);
```

**Lưu ý:**
- Cột `price` phải có kiểu `DECIMAL(10, 2)` hoặc `DOUBLE`
- Nếu bảng chưa có cột `price`, cần thêm:
  ```sql
  ALTER TABLE cart ADD COLUMN price DECIMAL(10, 2) NOT NULL DEFAULT 0;
  ```

---

## Tóm tắt thay đổi:

| File | Vấn đề | Giải pháp |
|------|--------|-----------|
| `cart/add.php` | Không lưu giá từ request | Lấy `$price` từ request và INSERT/UPDATE vào cart |
| `cart/view.php` | Trả về giá từ products | SELECT `c.price` từ cart và trả về trong response |
| `cart/update.php` | Không cập nhật giá | UPDATE cả `quantity` và `price` |

---

## Checklist:

- [ ] Kiểm tra bảng `cart` có cột `price` chưa
- [ ] Sửa `cart/add.php` để lưu giá từ request
- [ ] Sửa `cart/view.php` để trả về giá từ cart
- [ ] Sửa `cart/update.php` để cập nhật giá
- [ ] Test: Thêm sản phẩm với phiên bản đặc biệt → kiểm tra giá trong giỏ hàng
- [ ] Test: Update số lượng → kiểm tra giá vẫn đúng

---

## Sau khi sửa:

1. **Thêm sản phẩm với phiên bản đặc biệt:**
   - Android gửi: `{user_id, product_id, quantity: 1, price: 21000000}` (nếu chọn Trắng)
   - Backend lưu: `price = 21000000` vào bảng cart
   - Khi lấy giỏ hàng: Trả về `price = 21000000`

2. **Hiển thị trong giỏ hàng:**
   - Android nhận: `CartItem { price: 21000000 }`
   - Hiển thị: `21,000,000₫` (đúng giá đã tăng)


