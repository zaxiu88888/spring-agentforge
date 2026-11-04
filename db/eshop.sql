-- AgentForge eshop 演示库（DROP-CREATE 重建，保证种子确定性；SqlQueryTool 只读查询用）
DROP TABLE IF EXISTS order_item, orders, customer, product, category CASCADE;

CREATE TABLE category (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE product (
    id SERIAL PRIMARY KEY,
    category_id INT NOT NULL REFERENCES category(id),
    name TEXT NOT NULL,
    price NUMERIC(10,2) NOT NULL,
    stock INT DEFAULT 0
);

CREATE TABLE customer (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    city TEXT
);

CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    customer_id INT NOT NULL REFERENCES customer(id),
    status TEXT NOT NULL DEFAULT 'paid',     -- paid / shipped / completed / cancelled
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE order_item (
    id SERIAL PRIMARY KEY,
    order_id INT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id INT NOT NULL REFERENCES product(id),
    quantity INT NOT NULL
);

INSERT INTO category (id, name) VALUES
(1, '手机数码'), (2, '家用电器'), (3, '图书'), (4, '运动户外');

INSERT INTO product (id, category_id, name, price, stock) VALUES
(1,  1, '旗舰手机 Pro 256G',   5999.00, 120),
(2,  1, '轻薄笔记本 14 英寸',   4999.00, 80),
(3,  1, '降噪耳机 二代',         899.00, 300),
(4,  2, '变频空调 1.5 匹',      3199.00, 60),
(5,  2, '滚筒洗衣机 10KG',      2599.00, 45),
(6,  2, '扫地机器人',           1899.00, 90),
(7,  3, '三体（全集）',           99.00, 500),
(8,  3, '深入理解 Java 虚拟机', 129.00, 200),
(9,  4, '山地自行车 27 速',     1599.00, 35),
(10, 4, '折叠露营椅',             99.00, 400);

INSERT INTO customer (id, name, city) VALUES
(1, '陈晨', '深圳'), (2, '李锐', '杭州'), (3, '王雪', '北京'),
(4, '赵敏', '上海'), (5, '刘洋', '成都'), (6, '周婷', '广州');

INSERT INTO orders (id, customer_id, status, created_at) VALUES
(1,  1, 'completed', NOW() - INTERVAL '30 day'),
(2,  1, 'completed', NOW() - INTERVAL '25 day'),
(3,  2, 'completed', NOW() - INTERVAL '20 day'),
(4,  3, 'completed', NOW() - INTERVAL '18 day'),
(5,  2, 'shipped',   NOW() - INTERVAL '10 day'),
(6,  4, 'shipped',   NOW() - INTERVAL '8 day'),
(7,  5, 'paid',      NOW() - INTERVAL '5 day'),
(8,  3, 'paid',      NOW() - INTERVAL '3 day'),
(9,  6, 'paid',      NOW() - INTERVAL '2 day'),
(10, 6, 'cancelled', NOW() - INTERVAL '1 day');

INSERT INTO order_item (order_id, product_id, quantity) VALUES
-- 手机数码卖得最好
(1, 1, 2), (1, 3, 1),
(2, 2, 1),
(3, 1, 1), (3, 3, 2),
(4, 3, 3),
(5, 1, 1),
(6, 2, 2),
(7, 1, 1), (7, 9, 1),
(8, 3, 1), (8, 7, 2),
(9, 10, 4),
(10, 9, 1);
