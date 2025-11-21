# Checklist sau khi thêm cột price

## ✅ Đã hoàn thành:
- [x] Thêm cột `price` vào bảng `cart`
- [x] Thêm cột `price` vào bảng `order_items`

---

## 📋 Các bước tiếp theo:

### 1. **Cập nhật dữ liệu cũ trong bảng `cart`**

Nếu có dữ liệu cũ trong cart (giá = 0 hoặc NULL), cập nhật từ products:

```sql
UPDATE cart c
JOIN products p ON c.product_id = p.id
SET c.price = p.price
WHERE c.price = 0 OR c.price IS NULL;
```

**Lưu ý:** Dữ liệu cũ sẽ không có thông tin về phiên bản đặc biệt, nên sẽ dùng giá gốc.

---

### 2. **Cập nhật dữ liệu cũ trong bảng `order_items`** (Tùy chọn)

Nếu có đơn hàng cũ, có thể cập nhật:

```sql
UPDATE order_items oi
JOIN products p ON oi.product_id = p.id
SET oi.price = p.price
WHERE oi.price = 0 OR oi.price IS NULL;
```

**Lưu ý:** Dữ liệu cũ sẽ không chính xác vì không có thông tin về phiên bản đặc biệt.

---

### 3. **Kiểm tra code PHP đã được cập nhật**

Đảm bảo các file PHP đã được sửa theo hướng dẫn:

#### ✅ **cart/add.php**
- Lấy `$price` từ request
- INSERT/UPDATE với `price` từ request

#### ✅ **cart/view.php**
- SELECT `c.price` từ cart
- Trả về `price` trong response

#### ✅ **cart/update.php**
- Lấy `$price` từ request
- UPDATE cả `quantity` và `price`

#### ✅ **orders/place_order.php**
- SELECT `c.price` từ cart (hoặc dùng `COALESCE(c.price, p.price)`)
- INSERT vào `order_items` với `price` từ cart

#### ✅ **orders/list_user.php**
- SELECT `price` từ `order_items`
- Trả về `price` trong response

---

### 4. **Test lại toàn bộ flow**

#### Test 1: Thêm sản phẩm vào cart với phiên bản đặc biệt
1. Vào chi tiết sản phẩm
2. Chọn phiên bản "Trắng" (giá tăng 1,000,000₫)
3. Nhấn "Thêm vào giỏ hàng"
4. **Kiểm tra:** Vào giỏ hàng → Giá phải là giá đã tăng

#### Test 2: Xem giỏ hàng
1. Vào giỏ hàng
2. **Kiểm tra:** Giá hiển thị đúng (giá đã tăng nếu chọn phiên bản đặc biệt)

#### Test 3: Đặt hàng
1. Từ giỏ hàng, nhấn "Thanh toán"
2. Nhập địa chỉ và số điện thoại
3. Nhấn "Xác nhận"
4. **Kiểm tra:** Đặt hàng thành công

#### Test 4: Xem đơn hàng
1. Vào "Đơn hàng của tôi"
2. Mở đơn hàng vừa đặt
3. **Kiểm tra:** Giá trong đơn hàng phải đúng (giá đã tăng)

#### Test 5: Update số lượng trong cart
1. Vào giỏ hàng
2. Tăng/giảm số lượng
3. **Kiểm tra:** Giá vẫn đúng (không đổi về giá gốc)

---

### 5. **Kiểm tra database sau khi test**

#### Kiểm tra bảng `cart`:
```sql
SELECT 
    c.id,
    c.product_id,
    c.quantity,
    c.price AS cart_price,
    p.price AS product_price,
    (c.price - p.price) AS price_difference
FROM cart c
JOIN products p ON c.product_id = p.id
WHERE c.user_id = [USER_ID];
```

**Kỳ vọng:** 
- Nếu chọn phiên bản đặc biệt: `price_difference` = 1,000,000 (Trắng) hoặc 500,000 (Xám)
- Nếu không chọn: `price_difference` = 0

#### Kiểm tra bảng `order_items`:
```sql
SELECT 
    oi.id,
    oi.order_id,
    oi.product_id,
    oi.quantity,
    oi.price AS order_price,
    p.price AS product_price,
    (oi.price - p.price) AS price_difference
FROM order_items oi
JOIN products p ON oi.product_id = p.id
WHERE oi.order_id = [ORDER_ID];
```

**Kỳ vọng:** Giá trong `order_items` phải khớp với giá trong `cart` trước khi đặt hàng.

---

### 6. **Nếu vẫn còn lỗi**

#### Lỗi: Giá trong cart vẫn là giá cũ
- **Nguyên nhân:** Code PHP `cart/add.php` chưa lấy `price` từ request
- **Giải pháp:** Kiểm tra lại code `cart/add.php`

#### Lỗi: Giá trong đơn hàng vẫn là giá cũ
- **Nguyên nhân:** Code PHP `orders/place_order.php` chưa lấy `price` từ cart
- **Giải pháp:** Kiểm tra lại code `orders/place_order.php`

#### Lỗi: Không thể đặt hàng
- **Nguyên nhân:** Có thể do lỗi SQL hoặc transaction
- **Giải pháp:** 
  - Bật error reporting trong PHP
  - Kiểm tra log lỗi
  - Dùng code Version 3 (không dùng transaction) trong `FIX_ORDER_PLACE_ERROR.md`

---

## 📝 Tóm tắt:

1. ✅ Đã thêm cột `price` vào `cart` và `order_items`
2. ⏭️ Cập nhật dữ liệu cũ (nếu có)
3. ⏭️ Kiểm tra code PHP đã được cập nhật
4. ⏭️ Test lại toàn bộ flow
5. ⏭️ Kiểm tra database sau khi test

---

## 🎯 Kết quả mong đợi:

Sau khi hoàn thành tất cả các bước:
- ✅ Giá trong cart hiển thị đúng (đã tính theo phiên bản)
- ✅ Giá trong đơn hàng hiển thị đúng (đã tính theo phiên bản)
- ✅ Tổng tiền tính đúng
- ✅ Không còn lỗi khi đặt hàng


