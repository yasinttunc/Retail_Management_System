-- ============================================================
--  Retail Management System — MySQL schema + seed data
--  Structured data: stores, products, customers, inventory, orders
--  Table/column names match the JPA entity mapping used by the
--  Spring Boot backend (ddl-auto=update would produce the same).
-- ============================================================

CREATE DATABASE IF NOT EXISTS retail_management_system
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE retail_management_system;

-- Rebuild cleanly so the seed is idempotent -------------------
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS order_details;
DROP TABLE IF EXISTS inventory;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS customer;
DROP TABLE IF EXISTS store;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------- Schema ------------------------------------------
CREATE TABLE store (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE product (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(255) NOT NULL,
    category VARCHAR(255) NOT NULL,
    price    DOUBLE       NOT NULL,
    sku      VARCHAR(255) NOT NULL,
    CONSTRAINT uq_product_sku UNIQUE (sku)
) ENGINE=InnoDB;

CREATE TABLE customer (
    id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    name  VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(255) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE inventory (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id  BIGINT,
    store_id    BIGINT,
    stock_level INT,
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT fk_inventory_store   FOREIGN KEY (store_id)   REFERENCES store(id)
) ENGINE=InnoDB;

CREATE TABLE order_details (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT,
    store_id    BIGINT,
    total_price DOUBLE,
    date        DATETIME,
    CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
    CONSTRAINT fk_order_store    FOREIGN KEY (store_id)    REFERENCES store(id)
) ENGINE=InnoDB;

CREATE TABLE order_item (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT,
    product_id BIGINT,
    quantity   INT,
    price      DOUBLE,
    CONSTRAINT fk_item_order   FOREIGN KEY (order_id)   REFERENCES order_details(id),
    CONSTRAINT fk_item_product FOREIGN KEY (product_id) REFERENCES product(id)
) ENGINE=InnoDB;

-- ---------- Stores -----------------------------------------
INSERT INTO store (id, name, address) VALUES
 (1,  'Downtown Flagship',   '120 Market Street, San Francisco, CA'),
 (2,  'Westside Mall',       '4500 Sunset Blvd, Los Angeles, CA'),
 (3,  'North Point Center',  '78 Harbor Ave, Seattle, WA'),
 (5,  'Riverside Outlet',    '215 River Road, Austin, TX'),
 (7,  'Lakeview Store',      '901 Lakeshore Dr, Chicago, IL'),
 (8,  'Grand Central Shop',  '5 Park Avenue, New York, NY'),
 (10, 'Bayfront Electronics','330 Ocean Drive, Miami, FL');

-- ---------- Products ---------------------------------------
INSERT INTO product (id, name, category, price, sku) VALUES
 (1,  'Google Pixel 5',          'Mobile',               699.00,  'MOB-PIX5-001'),
 (2,  'Samsung Galaxy Z Fold 3', 'Mobile',              1799.00,  'MOB-ZFLD3-002'),
 (3,  'Apple iPhone 15',         'Mobile',               999.00,  'MOB-IP15-003'),
 (4,  'Sony WH-1000XM4',         'Accessories',          349.00,  'ACC-WH1KXM4-004'),
 (5,  'Bose QuietComfort 35 II', 'Accessories',          299.00,  'ACC-BOSEQC35-005'),
 (6,  'Logitech MX Master 3',    'Accessories',          99.99,   'ACC-MXM3-006'),
 (7,  'Dyson V11 Vacuum Cleaner','Home Appliances',      599.00,  'HOM-DYSV11-007'),
 (8,  'Philips Air Fryer XXL',   'Home Appliances',      249.00,  'HOM-PHAF-008'),
 (9,  'Nespresso Vertuo Next',   'Home Appliances',      179.00,  'HOM-NESPV-009'),
 (10, 'MacBook Pro 16"',         'Laptops and Monitors', 2499.00, 'LAP-MBP16-010'),
 (11, 'Dell XPS 13',             'Laptops and Monitors', 1299.00, 'LAP-XPS13-011'),
 (12, 'LG UltraFine 27" Monitor','Laptops and Monitors', 549.00,  'LAP-LGUF27-012'),
 (13, 'LG NanoCell 55" TV',      'TV and AV',            899.00,  'TV-LGNANO55-013'),
 (14, 'Sony Bravia 4K 65" TV',   'TV and AV',           1499.00,  'TV-SBRAV65-014'),
 (15, 'Samsung Soundbar Q800',   'TV and AV',            699.00,  'TV-SSBQ800-015');

-- ---------- Customers --------------------------------------
INSERT INTO customer (id, name, email, phone) VALUES
 (1, 'Alice Johnson',   'alice.johnson@example.com',   '+1-415-555-0111'),
 (2, 'Brian Smith',     'brian.smith@example.com',     '+1-213-555-0122'),
 (3, 'Carla Mendes',    'carla.mendes@example.com',    '+1-206-555-0133'),
 (4, 'David Kim',       'david.kim@example.com',       '+1-512-555-0144'),
 (5, 'Emma Wilson',     'emma.wilson@example.com',     '+1-312-555-0155'),
 (6, 'Frank Turner',    'frank.turner@example.com',    '+1-646-555-0166'),
 (7, 'Grace Lee',       'grace.lee@example.com',       '+1-305-555-0177'),
 (8, 'Hassan Ali',      'hassan.ali@example.com',      '+1-415-555-0188');

-- ---------- Inventory (stock per store) --------------------
INSERT INTO inventory (product_id, store_id, stock_level) VALUES
 (1,1,40),(3,1,25),(4,1,60),(6,1,120),(10,1,15),(13,1,10),
 (1,2,22),(2,2,8),(5,2,35),(8,2,18),(11,2,12),(14,2,6),
 (3,3,30),(4,3,45),(7,3,14),(9,3,26),(12,3,20),(15,3,16),
 (1,5,18),(6,5,90),(7,5,11),(8,5,22),(13,5,9),(15,5,13),
 (2,7,5),(4,7,50),(10,7,7),(11,7,10),(12,7,17),(14,7,4),
 (3,8,28),(5,8,40),(6,8,110),(9,8,24),(10,8,9),(13,8,8),
 (1,10,20),(4,10,55),(7,10,12),(8,10,19),(11,10,14),(14,10,5);

-- ---------- Orders (March 2025, matches report examples) ---
INSERT INTO order_details (id, customer_id, store_id, total_price, date) VALUES
 (1, 1, 1, 491.33, '2025-03-04 10:22:00'),
 (2, 2, 2, 211.93, '2025-03-06 14:05:00'),
 (3, 3, 3, 140.68, '2025-03-09 11:47:00'),
 (4, 4, 8, 837.03, '2025-03-11 16:30:00'),
 (5, 5, 5, 390.75, '2025-03-14 09:15:00'),
 (6, 6, 7, 233.63, '2025-03-18 13:52:00'),
 (7, 7, 10, 283.09, '2025-03-22 18:41:00'),
 (8, 8, 1, 129.98, '2025-04-02 12:10:00');

INSERT INTO order_item (order_id, product_id, quantity, price) VALUES
 (1, 1, 1, 149.97),(1, 4, 2, 89.98),(1, 6, 3, 89.97),
 (2, 2, 2, 79.98),(2, 5, 1, 49.99),
 (3, 3, 1, 60.68),(3, 4, 1, 44.99),
 (4, 10, 3, 189.97),(4, 4, 3, 119.97),
 (5, 7, 3, 89.97),(5, 8, 2, 79.98),
 (6, 11, 2, 159.98),(6, 12, 1, 73.65),
 (7, 5, 3, 269.97),(7, 6, 1, 13.12),
 (8, 1, 1, 129.98);

SELECT 'MySQL seed complete' AS status,
       (SELECT COUNT(*) FROM store)     AS stores,
       (SELECT COUNT(*) FROM product)   AS products,
       (SELECT COUNT(*) FROM customer)  AS customers,
       (SELECT COUNT(*) FROM inventory) AS inventory_rows,
       (SELECT COUNT(*) FROM order_details) AS orders;
