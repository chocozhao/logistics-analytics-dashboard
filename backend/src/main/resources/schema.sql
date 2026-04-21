-- H2-compatible schema aligned with the new CSV format

CREATE TABLE IF NOT EXISTS orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    client_id VARCHAR(20),
    order_id VARCHAR(50) UNIQUE,
    order_date DATE NOT NULL,
    delivery_date DATE,
    carrier VARCHAR(50),
    origin_city VARCHAR(100),
    destination_city VARCHAR(100),
    status VARCHAR(20),
    sku VARCHAR(50),
    product_category VARCHAR(50),
    quantity INTEGER,
    unit_price_usd DECIMAL(10,2),
    order_value_usd DECIMAL(10,2),
    is_promo BOOLEAN DEFAULT FALSE,
    promo_discount_pct DECIMAL(5,2) DEFAULT 0,
    region VARCHAR(20),
    warehouse VARCHAR(30)
);

CREATE INDEX IF NOT EXISTS idx_order_date ON orders(order_date);
CREATE INDEX IF NOT EXISTS idx_carrier ON orders(carrier);
CREATE INDEX IF NOT EXISTS idx_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_region ON orders(region);
CREATE INDEX IF NOT EXISTS idx_product_category ON orders(product_category);
CREATE INDEX IF NOT EXISTS idx_order_date_status ON orders(order_date, status);
CREATE INDEX IF NOT EXISTS idx_order_date_carrier ON orders(order_date, carrier);
