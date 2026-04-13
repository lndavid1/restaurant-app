# Báo Cáo Chi Tiết Chức Năng Hệ Thống Quản Lý Nhà Hàng

Bản báo cáo này chi tiết hóa quy trình thực hiện và tính năng của ba phân hệ cốt lõi: Quản lý bàn, Thực đơn (Menu), và Quản lý kho nguyên liệu.

---

## 1. Quản lý bàn (Table Management)
Chức năng này cho phép Quản trị viên (Admin) tổ chức và theo dõi tình trạng các bàn trong nhà hàng một cách trực quan.

### 1.1. Các tính năng chính
- Xem sơ đồ bàn: Hiển thị danh sách bàn dưới dạng lưới (Grid) với trạng thái thời gian thực.
- Thêm bàn mới: Thiết lập tên bàn (số bàn) và sức chứa (số người).
- Chỉnh sửa thông tin: Thay đổi tên hoặc sức chứa của bàn hiện có.
- Xóa bàn: Gỡ bỏ các bàn không còn sử dụng.
- Theo dõi trạng thái: Nhận biết bàn đang Trống (Available), Đang có khách (Occupied), hoặc Đã đặt (Reserved) qua mã màu.

### 1.2. Quy trình thực hiện chi tiết
1. Truy cập: Admin chọn Tab "Quản lý bàn" trên màn hình Admin Dashboard.
2. Tải dữ liệu: Hệ thống gọi API/Firestore để lấy danh sách bàn và sắp xếp theo thứ tự số bàn.
3. Thêm bàn:
    - Nhấn nút "Thêm bàn".
    - Nhập tên bàn (VD: Bàn 10) và sức chứa (VD: 4).
    - Hệ thống lưu vào cơ sở dữ liệu và cập nhật giao diện ngay lập tức.
4. Cập nhật trạng thái: Khi có đơn hàng được gán cho một bàn, trạng thái bàn tự động chuyển sang "Occupied" (màu đỏ). Khi thanh toán xong, bàn quay về trạng thái "Available" (màu xanh).

---

## 2. Quản lý Thực đơn (Menu Management)
Phân hệ giúp quản lý danh mục món ăn, giá cả và hình ảnh hiển thị cho khách hàng.

### 2.1. Các tính năng chính
- Danh sách món ăn: Hiển thị tên món, giá tiền, hình ảnh và thành phần.
- Tìm kiếm: Tìm nhanh món ăn theo tên.
- Phân loại (Category): Gắn món ăn vào các danh mục như Khai vị, Món chính, Đồ uống...
- Quản lý hình ảnh: Tải ảnh món ăn trực tiếp lên hệ thống (Firebase Storage).
- Cập nhật giá: Thay đổi giá bán linh hoạt.

### 2.2. Quy trình thực hiện chi tiết
1. Xem thực đơn: Truy cập Tab "Thực đơn". Admin có thể cuộn danh sách để xem tất cả các món.
2. Thêm món mới:
    - Nhấn "Thêm món".
    - Chọn ảnh từ thư viện điện thoại.
    - Nhập Tên món, Giá, Thành phần và chọn Danh mục.
    - Hệ thống tải ảnh lên Cloud Storage, lấy URL ảnh và lưu thông tin món ăn vào Firestore.
3. Chỉnh sửa: Nhấn "Sửa" trên món ăn tương ứng để cập nhật bất kỳ thông tin nào (bao gồm cả việc thay đổi ảnh mới).
4. Xóa món: Xác nhận xóa để gỡ món ăn khỏi menu bán hàng.

---

## 3. Quản lý Kho nguyên liệu (Ingredient Inventory)
Chức năng này thường được thực hiện bởi bộ phận Bếp (Kitchen) hoặc Quản lý kho để đảm bảo nguồn cung thực phẩm ổn định.

### 3.1. Các tính năng chính
- Theo dõi tồn kho: Xem số lượng tồn kho theo đơn vị tính (kg, lít, cái...).
- Nhập kho mới: Thêm nguyên liệu mới vào danh sách quản lý.
- Cập nhật số lượng: Điều chỉnh tăng/giảm lượng tồn kho khi nhập thêm hoặc sau mỗi ngày làm việc.
- Cảnh báo tồn thấp: Tự động đánh dấu màu đỏ các nguyên liệu có số lượng dưới ngưỡng an toàn (VD: < 5).

### 3.2. Quy trình thực hiện chi tiết
1. Kiểm kho: Nhân viên bếp truy cập Tab "Nguyên liệu" trên Kitchen Dashboard.
2. Nhập kho:
    - Nhấn "Nhập kho".
    - Nhập tên nguyên liệu (Thịt bò, Hành tây...), Đơn vị (kg) và Số lượng ban đầu.
3. Cảnh báo: Hệ thống kiểm tra giá trị `stock`. Nếu `stock < 5`, giao diện sẽ hiển thị biểu tượng cảnh báo màu đỏ để nhân viên kịp thời mua thêm.
4. Điều chỉnh: Khi nhập hàng mới, nhân viên nhấn "Sửa" và cập nhật số lượng tồn kho mới.
5. Xóa: Loại bỏ các nguyên loại không còn nhập hoặc không dùng trong chế biến.

---

## Công nghệ sử dụng
- Cơ sở dữ liệu: Google Firestore (Lưu trữ dữ liệu dạng Document theo thời gian thực).
- Lưu trữ hình ảnh: Firebase Storage.
- Giao diện: Jetpack Compose (Kotlin) với thiết kế hiện đại, Material Design 3.
