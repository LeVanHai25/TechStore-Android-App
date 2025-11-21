# ✅ GIẢI THÍCH: Logic Khuyến mãi + Phiên bản đặc biệt

## 🔍 Flow hoàn chỉnh:

### 1️⃣ **Thêm vào giỏ hàng (ProductDetailActivity)**
```
User chọn phiên bản "Trắng" (+1,000,000₫)
→ finalPrice = basePrice + 1,000,000
→ Gửi CartRequest(price = finalPrice) lên backend
→ Backend lưu vào cart.price = finalPrice ✅
```

### 2️⃣ **Hiển thị giỏ hàng (CartActivity)**
```
Load cart từ backend
→ cart.price = 31,000,000₫ (đã có giá phiên bản) ✅
→ originalTotal = 31,000,000₫
→ Áp dụng khuyến mãi 10%:
   discount = 31,000,000 * 10% = 3,100,000₫
   finalTotal = 31,000,000 - 3,100,000 = 27,900,000₫ ✅
```

### 3️⃣ **Đặt hàng (place_order.php)**

#### ✅ **order_items** (Chi tiết từng sản phẩm):
```php
// Lấy từ cart (đã có giá phiên bản)
SELECT c.price FROM cart WHERE user_id = ?
→ cart.price = 31,000,000₫ (đã có giá phiên bản) ✅

// INSERT vào order_items
INSERT INTO order_items (price) VALUES (31,000,000)
→ order_items.price = 31,000,000₫ ✅ (KHÔNG ảnh hưởng)
```

#### ✅ **orders** (Tổng tiền đơn hàng):
```php
// Nhận từ client (đã áp dụng khuyến mãi)
$total = $data["total"]; // = 27,900,000₫

// INSERT vào orders
INSERT INTO orders (total) VALUES (27,900,000)
→ orders.total = 27,900,000₫ ✅ (Đã áp dụng khuyến mãi)
```

---

## 📊 **Kết quả trong database:**

### Bảng `order_items`:
| product_id | quantity | price |
|------------|----------|-------|
| 1          | 1        | 31,000,000₫ | ← Giá đã có phiên bản ✅

### Bảng `orders`:
| id | user_id | total | status |
|----|---------|-------|--------|
| 1  | 1       | 27,900,000₫ | Chờ xử lý | ← Đã áp dụng khuyến mãi ✅

---

## ✅ **KẾT LUẬN:**

### **Phiên bản đặc biệt:**
- ✅ `order_items.price` = lấy từ `cart.price` (đã có giá phiên bản)
- ✅ **KHÔNG ảnh hưởng** bởi việc sửa `place_order.php`

### **Khuyến mãi:**
- ✅ `orders.total` = nhận từ client (đã áp dụng khuyến mãi)
- ✅ **Hoạt động đúng** sau khi sửa `place_order.php`

---

## 🎯 **Tóm tắt:**

| Thành phần | Giá trị | Nguồn | Ảnh hưởng? |
|------------|---------|-------|------------|
| `order_items.price` | 31,000,000₫ | `cart.price` (có phiên bản) | ❌ KHÔNG |
| `orders.total` | 27,900,000₫ | Client (đã giảm 10%) | ✅ ĐÚNG |

**→ Cả hai đều hoạt động đúng và độc lập với nhau!**


