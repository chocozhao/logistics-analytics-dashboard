-- Database initialization script for Logistics Analytics Dashboard
-- New schema based on mock_logistics_data.csv

CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(20),
    order_id VARCHAR(50) UNIQUE,
    order_date DATE NOT NULL,
    promised_delivery_date DATE NOT NULL,
    delivery_date DATE,  -- NULL for in_transit / canceled
    carrier VARCHAR(50),
    origin_city VARCHAR(100),
    destination_city VARCHAR(100),
    status VARCHAR(20) CHECK (status IN ('delivered', 'delayed', 'in_transit', 'exception', 'canceled')),
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

-- Performance indexes
CREATE INDEX IF NOT EXISTS idx_order_date ON orders(order_date);
CREATE INDEX IF NOT EXISTS idx_promised_delivery_date ON orders(promised_delivery_date);
CREATE INDEX IF NOT EXISTS idx_delivery_date ON orders(delivery_date);
CREATE INDEX IF NOT EXISTS idx_carrier ON orders(carrier);
CREATE INDEX IF NOT EXISTS idx_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_region ON orders(region);
CREATE INDEX IF NOT EXISTS idx_product_category ON orders(product_category);
CREATE INDEX IF NOT EXISTS idx_order_date_status ON orders(order_date, status);
CREATE INDEX IF NOT EXISTS idx_order_date_carrier ON orders(order_date, carrier);

COMMENT ON TABLE orders IS 'Main orders table for logistics analytics dashboard';
COMMENT ON COLUMN orders.promised_delivery_date IS 'Expected delivery date';
COMMENT ON COLUMN orders.delivery_date IS 'NULL means not yet delivered (in_transit or canceled)';
COMMENT ON COLUMN orders.status IS 'Values: delivered, delayed, in_transit, exception, canceled';
COMMENT ON COLUMN orders.region IS 'Values: US-W, US-C, US-E, EU, UK';
