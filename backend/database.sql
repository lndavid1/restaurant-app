CREATE DATABASE IF NOT EXISTS restaurant_db;
USE restaurant_db;

-- 1. XÓA BẢNG CŨ (Để reset sạch sẽ)
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS logs, settings, notifications, payments, invoices, order_details, orders, reservations, restaurant_tables, products, categories, users, roles;
SET FOREIGN_KEY_CHECKS = 1;

-- 2. PHÂN QUYỀN (ROLES)
CREATE TABLE roles (
    id INT PRIMARY KEY AUTO_INCREMENT,
    role_name VARCHAR(20) NOT NULL
);
INSERT INTO roles (id, role_name) VALUES (1, 'admin'), (2, 'customer'), (3, 'employee');

-- 3. NGƯỜI DÙNG (USERS)
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    role_id INT,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(15),
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- 4. DANH MỤC (CATEGORIES)
CREATE TABLE categories (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    image_url VARCHAR(255)
);
INSERT INTO categories (id, name) VALUES (1, 'Món chính'), (2, 'Món phụ'), (3, 'Đồ uống'), (4, 'Tráng miệng');

-- 5. SẢN PHẨM (PRODUCTS) - CẬP NHẬT GIÁ VNĐ
CREATE TABLE products (
    id INT PRIMARY KEY AUTO_INCREMENT,
    category_id INT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price BIGINT NOT NULL, -- VNĐ dùng số nguyên lớn
    image_url VARCHAR(255),
    is_available INT DEFAULT 1,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

INSERT INTO products (category_id, name, description, price) VALUES
(1, 'Phở Bò Đặc Biệt', 'Nước dùng ninh xương 12 tiếng, thịt bò tươi.', 55000),
(1, 'Bún Chả Hà Nội', 'Thịt nướng than hoa, nước mắm chua ngọt.', 50000),
(2, 'Nem Rán', 'Nem rán giòn rụm, ăn kèm rau sống.', 30000),
(3, 'Trà Đá', 'Trà đá giải nhiệt.', 5000),
(3, 'Cà Phê Sữa Đá', 'Cà phê pha phin truyền thống.', 25000);

-- 6. BÀN ĂN (TABLES)
CREATE TABLE restaurant_tables (
    id INT PRIMARY KEY AUTO_INCREMENT,
    table_number VARCHAR(10) NOT NULL,
    capacity INT NOT NULL,
    status ENUM('available', 'occupied', 'reserved') DEFAULT 'available'
);
INSERT INTO restaurant_tables (table_number, capacity) VALUES ('Bàn 1', 2), ('Bàn 2', 4), ('Bàn 3', 4), ('Bàn 4', 6), ('Bàn 5', 8), ('Bàn 6', 10);

-- 7. ĐƠN HÀNG (ORDERS)
CREATE TABLE orders (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT,
    table_id INT,
    total_amount BIGINT NOT NULL,
    order_status ENUM('pending', 'processing', 'completed', 'cancelled') DEFAULT 'pending',
    payment_status ENUM('unpaid', 'paid') DEFAULT 'unpaid',
    vnp_txn_ref VARCHAR(100),
    payos_order_code BIGINT DEFAULT NULL,   -- Mã đơn PayOS để đối soát webhook
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (table_id) REFERENCES restaurant_tables(id)
);

-- 8. CHI TIẾT ĐƠN HÀNG (ORDER DETAILS)
CREATE TABLE order_details (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT,
    product_id INT,
    quantity INT NOT NULL,
    price BIGINT NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- 9. THANH TOÁN (PAYMENTS)
CREATE TABLE payments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT,
    vnp_transaction_no VARCHAR(100),
    vnp_bank_code VARCHAR(50),
    vnp_pay_date DATETIME,
    amount BIGINT,
    card_type VARCHAR(20),
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- 10. CẤU HÌNH HỆ THỐNG (SETTINGS)
CREATE TABLE settings (
    id INT PRIMARY KEY AUTO_INCREMENT,
    key_name VARCHAR(100) UNIQUE,
    key_value TEXT
);
INSERT INTO settings (key_name, key_value) VALUES
('openai_api_key', 'YOUR_KEY_HERE'),
('vnp_tmn_code', '2QXG2YQ8'),
('vnp_hash_secret', 'ONLREBTMUPURMREBWZBIXFOCBSNAGSST');

-- 11. KHO NGUYÊN LIỆU (INVENTORY)
CREATE TABLE ingredients (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    stock DECIMAL(10,2) DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
INSERT INTO ingredients (name, unit, stock) VALUES 
('Thịt bò', 'kg', 15.5),
('Gạo', 'kg', 50.0),
('Rau xanh', 'kg', 10.0),
('Cà phê', 'Gói', 20.0);

-- ── MIGRATION: Thêm cột PayOS (chạy nếu DB đã tồn tại, bỏ qua nếu tạo mới) ──
-- ALTER TABLE orders ADD COLUMN IF NOT EXISTS payos_order_code BIGINT DEFAULT NULL;
