# 🎁 HỆ THỐNG KHUYẾN MÃI - PHP BACKEND CODE

## 📌 1. Tạo bảng promotions trong database

```sql
CREATE TABLE promotions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    min_amount BIGINT NOT NULL,
    discount_percent INT NOT NULL,
    description VARCHAR(255),
    status TINYINT DEFAULT 1,  -- 1: hoạt động, 0: tắt
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Thêm 4 ưu đãi mặc định
INSERT INTO promotions (min_amount, discount_percent, description) VALUES
(10000000, 5, 'Giảm 5% cho đơn hàng trên 10 triệu'),
(20000000, 10, 'Giảm 10% cho đơn hàng trên 20 triệu'),
(30000000, 15, 'Giảm 15% cho đơn hàng trên 30 triệu'),
(50000000, 20, 'Giảm 20% cho đơn hàng trên 50 triệu');
```

---

## 📌 2. File: promotions/get_all.php

```php
<?php
header("Content-Type: application/json");
require_once "../config/config.php";

try {
    $stmt = $conn->prepare("SELECT * FROM promotions WHERE status = 1 ORDER BY min_amount ASC");
    $stmt->execute();
    $promotions = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    echo json_encode([
        "success" => true,
        "data" => $promotions
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

## 📌 3. File: promotions/add.php

```php
<?php
header("Content-Type: application/json");
require_once "../config/config.php";

$data = json_decode(file_get_contents("php://input"), true);

if (!isset($data["min_amount"]) || !isset($data["discount_percent"])) {
    echo json_encode([
        "success" => false,
        "message" => "Thiếu thông tin bắt buộc"
    ]);
    exit;
}

$min_amount = $data["min_amount"] ?? 0;
$discount_percent = $data["discount_percent"] ?? 0;
$description = $data["description"] ?? "";
$status = $data["status"] ?? 1;

if ($min_amount <= 0 || $discount_percent <= 0 || $discount_percent > 100) {
    echo json_encode([
        "success" => false,
        "message" => "Giá trị không hợp lệ"
    ]);
    exit;
}

try {
    $stmt = $conn->prepare("
        INSERT INTO promotions (min_amount, discount_percent, description, status)
        VALUES (?, ?, ?, ?)
    ");
    
    $stmt->execute([$min_amount, $discount_percent, $description, $status]);
    
    echo json_encode([
        "success" => true,
        "message" => "Thêm khuyến mãi thành công"
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

## 📌 4. File: promotions/update.php

```php
<?php
header("Content-Type: application/json");
require_once "../config/config.php";

$data = json_decode(file_get_contents("php://input"), true);

if (!isset($data["id"]) || !isset($data["min_amount"]) || !isset($data["discount_percent"])) {
    echo json_encode([
        "success" => false,
        "message" => "Thiếu thông tin bắt buộc"
    ]);
    exit;
}

$id = $data["id"] ?? 0;
$min_amount = $data["min_amount"] ?? 0;
$discount_percent = $data["discount_percent"] ?? 0;
$description = $data["description"] ?? "";

if ($id <= 0 || $min_amount <= 0 || $discount_percent <= 0 || $discount_percent > 100) {
    echo json_encode([
        "success" => false,
        "message" => "Giá trị không hợp lệ"
    ]);
    exit;
}

try {
    $stmt = $conn->prepare("
        UPDATE promotions 
        SET min_amount = ?, discount_percent = ?, description = ?
        WHERE id = ?
    ");
    
    $stmt->execute([$min_amount, $discount_percent, $description, $id]);
    
    if ($stmt->rowCount() > 0) {
        echo json_encode([
            "success" => true,
            "message" => "Cập nhật khuyến mãi thành công"
        ]);
    } else {
        echo json_encode([
            "success" => false,
            "message" => "Không tìm thấy khuyến mãi để cập nhật"
        ]);
    }
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

## 📌 5. File: promotions/delete.php

```php
<?php
header("Content-Type: application/json");
require_once "../config/config.php";

$id = $_GET["id"] ?? 0;

if ($id <= 0) {
    echo json_encode([
        "success" => false,
        "message" => "ID không hợp lệ"
    ]);
    exit;
}

try {
    $stmt = $conn->prepare("DELETE FROM promotions WHERE id = ?");
    $stmt->execute([$id]);
    
    if ($stmt->rowCount() > 0) {
        echo json_encode([
            "success" => true,
            "message" => "Xóa khuyến mãi thành công"
        ]);
    } else {
        echo json_encode([
            "success" => false,
            "message" => "Không tìm thấy khuyến mãi để xóa"
        ]);
    }
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

## 📌 6. File: promotions/get_best_promotion.php

```php
<?php
header("Content-Type: application/json");
require_once "../config/config.php";

$total = $_GET["total"] ?? 0;

if ($total <= 0) {
    echo json_encode([
        "success" => true,
        "promotion" => null
    ]);
    exit;
}

try {
    // Lấy khuyến mãi tốt nhất (min_amount <= total, sắp xếp theo min_amount DESC)
    $stmt = $conn->prepare("
        SELECT * FROM promotions
        WHERE min_amount <= ? AND status = 1
        ORDER BY min_amount DESC
        LIMIT 1
    ");
    
    $stmt->execute([$total]);
    $promo = $stmt->fetch(PDO::FETCH_ASSOC);
    
    echo json_encode([
        "success" => true,
        "promotion" => $promo ? $promo : null
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

## 📌 7. Cấu trúc thư mục

```
backend/
├── config/
│   └── config.php
└── promotions/
    ├── get_all.php
    ├── add.php
    ├── update.php
    ├── delete.php
    └── get_best_promotion.php
```

---

## ✅ HƯỚNG DẪN SỬ DỤNG

1. **Tạo bảng promotions** trong database (chạy SQL ở trên)
2. **Copy các file PHP** vào thư mục `backend/promotions/`
3. **Test API** bằng Postman hoặc browser:
   - `GET http://localhost/backend/promotions/get_all.php`
   - `GET http://localhost/backend/promotions/get_best_promotion.php?total=25000000`
4. **Kiểm tra Android app** đã kết nối đúng API endpoint

---

## 🎯 LƯU Ý

- Tất cả API đều trả về JSON
- `get_best_promotion.php` trả về `promotion: null` nếu không có khuyến mãi phù hợp
- `status = 1` là khuyến mãi đang hoạt động
- `min_amount` và `discount_percent` phải > 0, `discount_percent` <= 100


