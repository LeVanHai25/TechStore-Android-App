# 🔧 SỬA LỖI: Khuyến mãi không được áp dụng vào đơn hàng

## ❌ Vấn đề:
- Khi ở giỏ hàng đã giảm 10% (hiển thị đúng)
- Nhưng khi thanh toán và vào order lại vẫn là giá cũ chưa được giảm

## 🔍 Nguyên nhân:
1. **Android:** `OrderRequest` không có field `total` để gửi tổng tiền đã giảm
2. **Backend PHP:** `place_order.php` tự tính tổng tiền từ cart, không nhận `total` từ client

## ✅ Giải pháp:

### 1. Android - Đã sửa:
- ✅ Thêm field `total` vào `OrderRequest.java`
- ✅ Gửi `orderTotal` (đã giảm) trong `CartActivity.java`

### 2. Backend PHP - Cần sửa:

**File: `orders/place_order.php`**

```php
<?php
header("Content-Type: application/json");
require_once "../config/config.php";

$data = json_decode(file_get_contents("php://input"), true);

$user_id = $data["user_id"] ?? null;
$address = trim($data["address"] ?? "");
$phone = trim($data["phone"] ?? "");
$note = trim($data["note"] ?? "");
$total = $data["total"] ?? null; // QUAN TRỌNG: Nhận total từ client (đã áp dụng khuyến mãi)

if (!$user_id || $address === "" || $phone === "") {
    echo json_encode([
        "success" => false,
        "message" => "Vui lòng nhập đầy đủ thông tin địa chỉ và số điện thoại."
    ]);
    exit;
}

try {
    // Bắt đầu transaction
    $conn->beginTransaction();

    // 1. Lấy tất cả items từ cart với giá đã tính
    $stmt = $conn->prepare("
        SELECT 
            c.product_id,
            c.quantity,
            COALESCE(c.price, p.price) AS price,  -- Giá từ cart (đã tính theo phiên bản)
            p.name AS product_name,
            p.image
        FROM cart c
        JOIN products p ON c.product_id = p.id
        WHERE c.user_id = ?
    ");
    $stmt->execute([$user_id]);
    $cartItems = $stmt->fetchAll(PDO::FETCH_ASSOC);

    if (!$cartItems || count($cartItems) === 0) {
        $conn->rollBack();
        echo json_encode([
            "success" => false, 
            "message" => "Giỏ hàng trống."
        ]);
        exit;
    }

    // 2. QUAN TRỌNG: Nếu client gửi total, dùng total đó (đã áp dụng khuyến mãi)
    // Nếu không, tính từ cart (fallback)
    if ($total === null || $total <= 0) {
        // Fallback: Tính tổng tiền từ giá trong cart
        $total = 0;
        foreach ($cartItems as $item) {
            $total += (float)$item["price"] * (int)$item["quantity"];
        }
    }

    // 3. Tạo đơn hàng với total đã áp dụng khuyến mãi
    $stmt = $conn->prepare("
        INSERT INTO orders (user_id, address, phone, note, total, status, created_at) 
        VALUES (?, ?, ?, ?, ?, 'Chờ xử lý', NOW())
    ");
    $stmt->execute([$user_id, $address, $phone, $note, $total]);
    $orderId = $conn->lastInsertId();

    // 4. Tạo order_items với giá từ cart (QUAN TRỌNG)
    $stmt = $conn->prepare("
        INSERT INTO order_items (order_id, product_id, quantity, price)
        VALUES (?, ?, ?, ?)
    ");
    
    foreach ($cartItems as $item) {
        $stmt->execute([
            $orderId,
            $item["product_id"],
            $item["quantity"],
            $item["price"]  // Giá từ cart (đã tính theo phiên bản)
        ]);
    }

    // 5. Xóa giỏ hàng
    $stmt = $conn->prepare("DELETE FROM cart WHERE user_id = ?");
    $stmt->execute([$user_id]);

    // Commit transaction
    $conn->commit();

    echo json_encode([
        "success" => true,
        "message" => "Đặt hàng thành công.",
        "order_id" => $orderId
    ]);
    
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

---

## 📋 Thay đổi quan trọng:

### Backend PHP (`orders/place_order.php`):

**TRƯỚC:**
```php
// Tính tổng tiền từ cart
$total = 0;
foreach ($cartItems as $item) {
    $total += (float)$item["price"] * (int)$item["quantity"];
}
```

**SAU:**
```php
// QUAN TRỌNG: Nhận total từ client (đã áp dụng khuyến mãi)
$total = $data["total"] ?? null;

// Nếu client gửi total, dùng total đó
// Nếu không, tính từ cart (fallback)
if ($total === null || $total <= 0) {
    $total = 0;
    foreach ($cartItems as $item) {
        $total += (float)$item["price"] * (int)$item["quantity"];
    }
}
```

---

## ✅ Checklist:

- [x] Android: Thêm field `total` vào `OrderRequest.java`
- [x] Android: Gửi `orderTotal` trong `CartActivity.java`
- [ ] Backend: Sửa `orders/place_order.php` để nhận `total` từ client
- [ ] Test: Đặt hàng với khuyến mãi và kiểm tra total trong order

---

## 🎯 Kết quả mong đợi:

1. User áp dụng khuyến mãi 10% trong giỏ hàng
2. Tổng tiền hiển thị: 27,000,000₫ (giảm từ 30,000,000₫)
3. Khi thanh toán, gửi `total: 27000000` lên backend
4. Backend lưu `total: 27000000` vào bảng `orders`
5. Trong order history, hiển thị đúng: 27,000,000₫


