-- H2-compatible schema for development
-- Based on the PostgreSQL schema from database/init.sql

CREATE TABLE IF NOT EXISTS orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_date DATE NOT NULL,
    promised_delivery_date DATE NOT NULL,
    actual_delivery_date DATE,  -- NULL for not yet delivered
    status VARCHAR(20),
    carrier VARCHAR(50),
    destination_city VARCHAR(100),
    destination_state VARCHAR(50),
    destination_region VARCHAR(50),
    order_value DECIMAL(10,2),
    sku VARCHAR(50),
    quantity INTEGER
);

-- Create indexes (H2 syntax)
CREATE INDEX IF NOT EXISTS idx_order_date ON orders(order_date);
CREATE INDEX IF NOT EXISTS idx_carrier ON orders(carrier);
CREATE INDEX IF NOT EXISTS idx_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_delivery_dates ON orders(promised_delivery_date, actual_delivery_date);
CREATE INDEX IF NOT EXISTS idx_region ON orders(destination_region);
CREATE INDEX IF NOT EXISTS idx_order_date_status ON orders(order_date, status);