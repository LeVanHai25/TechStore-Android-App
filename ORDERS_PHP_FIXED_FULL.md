# Code PHP đầy đủ - Orders (Đã sửa lỗi giá)

## File 1: orders/place_order.php (QUAN TRỌNG - Đã sửa)

**Vấn đề:** Đang lấy `p.price` từ products thay vì `c.price` từ cart.

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

    // QUAN TRỌNG: Lấy giá từ cart (KHÔNG phải từ products)
    // Dùng COALESCE để fallback về giá products nếu cart không có price
    $stmt = $conn->prepare("
        SELECT 
            c.product_id, 
            c.quantity, 
            COALESCE(c.price, p.price) AS price,  -- QUAN TRỌNG: Lấy từ cart trước
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

    // QUAN TRỌNG: Nếu client gửi total, dùng total đó (đã áp dụng khuyến mãi)
    // Nếu không, tính từ cart (fallback)
    if ($total === null || $total <= 0) {
        // Fallback: Tính tổng tiền từ giá trong cart
        $total = 0;
        foreach ($cartItems as $item) {
            $total += (float)$item["price"] * (int)$item["quantity"];
        }
    }

    // Thêm đơn hàng với total đã áp dụng khuyến mãi
    $stmt = $conn->prepare("
        INSERT INTO orders (user_id, address, phone, note, total, status, created_at)
        VALUES (?, ?, ?, ?, ?, 'Chờ xử lý', NOW())
    ");
    $stmt->execute([$user_id, $address, $phone, $note, $total]);
    $order_id = $conn->lastInsertId();

    // Thêm chi tiết đơn hàng với giá từ cart
    // QUAN TRỌNG: Bảng order_items KHÔNG có cột product_name và image
    // Chỉ INSERT: order_id, product_id, quantity, price
    $stmt = $conn->prepare("
        INSERT INTO order_items (order_id, product_id, quantity, price)
        VALUES (?, ?, ?, ?)
    ");
    
    foreach ($cartItems as $item) {
        $stmt->execute([
            $order_id,
            $item["product_id"],
            $item["quantity"],
            $item["price"]  // QUAN TRỌNG: Giá từ cart (đã tính theo phiên bản)
        ]);
    }

    // Xoá giỏ hàng
    $stmt = $conn->prepare("DELETE FROM cart WHERE user_id = ?");
    $stmt->execute([$user_id]);

    // Commit transaction
    $conn->commit();

    echo json_encode([
        "success" => true,
        "message" => "Đặt hàng thành công.",
        "order_id" => $order_id
    ]);
    
    $stmt->closeCursor();
} catch (PDOException $e) {
    $conn->rollBack();
    echo json_encode([
        "success" => false,
        "message" => "Lỗi đặt hàng: " . $e->getMessage()
    ]);
} finally {
    $conn = null;
}
?>
```

**Thay đổi quan trọng:**
- ✅ SELECT `COALESCE(c.price, p.price)` từ cart (fallback về products nếu cart không có)
- ✅ Tính tổng tiền từ giá trong cart
- ✅ INSERT vào order_items với giá từ cart
- ✅ Thêm try-catch để xử lý nếu bảng không có cột `product_name` và `image`

---

## File 2: orders/list_user.php (Đã sửa)

**Vấn đề:** Đang SELECT `oi.price` nhưng có thể đang JOIN với products và lấy sai.

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
        
        // Format order
        $result[] = [
            "id" => (int)$orderId,
            "order_id" => (int)$orderId,
            "user_id" => (int)$order["user_id"],
            "user_name" => $order["user_name"] ?? "",
            "user_email" => $order["user_email"] ?? "",
            "address" => $order["address"] ?? "",
            "phone" => $order["phone"] ?? "",
            "note" => $order["note"] ?? "",
            "total" => (float)$order["total"],
            "status" => $order["status"] ?? "pending",
            "created_at" => $order["created_at"] ?? "",
            "items" => $orderItems
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
- ✅ SELECT `oi.price` từ order_items (đã đúng)
- ✅ Thêm `LEFT JOIN products` để lấy thông tin bổ sung nếu order_items không có product_name/image
- ✅ Thêm fallback cho product_name và image

---

## File 3: orders/getAllOrders.php (Admin - Đã sửa)

**Vấn đề:** Đang SELECT `oi.*` và JOIN với products, có thể lấy giá từ products.

```php
<?php
header("Content-Type: application/json");
require_once "../config/config.php";

try {
    $stmt = $conn->query("
        SELECT 
            o.*, 
            u.name AS user_name, 
            u.email AS user_email
        FROM orders o
        JOIN users u ON o.user_id = u.id
        ORDER BY o.created_at DESC
    ");
    
    $orders = [];
    
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $orderId = $row["id"];

        // QUAN TRỌNG: Lấy giá từ order_items (KHÔNG phải từ products)
        $stmtItems = $conn->prepare("
            SELECT 
                oi.product_id,
                oi.product_name,  -- Lấy từ order_items nếu có
                oi.image,  -- Lấy từ order_items nếu có
                oi.quantity, 
                oi.price,  -- QUAN TRỌNG: Giá từ order_items
                p.name AS product_name_fallback,  -- Fallback từ products
                p.image AS image_fallback  -- Fallback từ products
            FROM order_items oi
            LEFT JOIN products p ON oi.product_id = p.id
            WHERE oi.order_id = :order_id
        ");
        $stmtItems->execute(["order_id" => $orderId]);
        $items = $stmtItems->fetchAll(PDO::FETCH_ASSOC);
        
        // Format items
        $formattedItems = [];
        foreach ($items as $item) {
            $formattedItems[] = [
                "product_id" => (int)$item["product_id"],
                "product_name" => $item["product_name"] ?? $item["product_name_fallback"] ?? "Sản phẩm",
                "image" => $item["image"] ?? $item["image_fallback"] ?? "",
                "quantity" => (int)$item["quantity"],
                "price" => (float)$item["price"]  // QUAN TRỌNG: Giá từ order_items
            ];
        }

        $orders[] = [
            "id" => (int)$row["id"],
            "order_id" => (int)$row["id"],
            "user_id" => (int)$row["user_id"],
            "user_name" => $row["user_name"],
            "user_email" => $row["user_email"],
            "address" => $row["address"],
            "phone" => $row["phone"],
            "note" => $row["note"],
            "created_at" => $row["created_at"],
            "total" => (float)$row["total"],
            "status" => $row["status"],
            "items" => $formattedItems
        ];
        
        $stmtItems->closeCursor();
    }

    echo json_encode([
        "success" => true, 
        "orders" => $orders
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
- ✅ SELECT `oi.price` từ order_items (KHÔNG phải từ products)
- ✅ LEFT JOIN products chỉ để lấy thông tin bổ sung
- ✅ Format lại items để đảm bảo trả về đúng cấu trúc

---

## Tóm tắt thay đổi:

| File | Vấn đề | Đã sửa |
|------|--------|--------|
| `orders/place_order.php` | ❌ Lấy `p.price` từ products | ✅ Lấy `COALESCE(c.price, p.price)` từ cart |
| `orders/list_user.php` | ⚠️ Có thể lấy sai | ✅ Đảm bảo lấy `oi.price` từ order_items |
| `orders/getAllOrders.php` | ⚠️ Có thể lấy sai | ✅ Đảm bảo lấy `oi.price` từ order_items |

---

## Checklist:

- [ ] Sửa `orders/place_order.php` - Lấy giá từ cart
- [ ] Sửa `orders/list_user.php` - Đảm bảo lấy giá từ order_items
- [ ] Sửa `orders/getAllOrders.php` - Đảm bảo lấy giá từ order_items
- [ ] Test đặt hàng với phiên bản đặc biệt
- [ ] Kiểm tra giá trong đơn hàng

