CREATE TABLE IF NOT EXISTS orders (
    id SERIAL PRIMARY KEY,
    order_date DATE NOT NULL,
    promised_delivery_date DATE NOT NULL,
    actual_delivery_date DATE,  -- NULL for not yet delivered
    status VARCHAR(20) CHECK (status IN ('pending', 'in_transit', 'delivered', 'cancelled')),
    carrier VARCHAR(50),
    destination_city VARCHAR(100),
    destination_state VARCHAR(50),
    destination_region VARCHAR(50),
    order_value DECIMAL(10,2),
    sku VARCHAR(50),
    quantity INTEGER
);

-- Performance indexes
CREATE INDEX IF NOT EXISTS idx_order_date ON orders(order_date);
CREATE INDEX IF NOT EXISTS idx_carrier ON orders(carrier);
CREATE INDEX IF NOT EXISTS idx_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_delivery_dates ON orders(promised_delivery_date, actual_delivery_date);
CREATE INDEX IF NOT EXISTS idx_region ON orders(destination_region);
CREATE INDEX IF NOT EXISTS idx_order_date_status ON orders(order_date, status);

-- Comments
COMMENT ON TABLE orders IS 'Main orders table for logistics analytics dashboard';
COMMENT ON COLUMN orders.actual_delivery_date IS 'NULL means not yet delivered';
COMMENT ON COLUMN orders.status IS 'Valid values: pending, in_transit, delivered, cancelled';

-- Derived fields (computed in queries):
-- is_delayed = (actual_delivery_date > promised_delivery_date)
-- delivery_days = actual_delivery_date - order_date