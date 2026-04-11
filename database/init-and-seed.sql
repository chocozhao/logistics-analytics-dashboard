-- SQL script to initialize database and seed sample data
-- Run this on your PostgreSQL production database to ensure tables exist with data
-- This script is idempotent: it won't recreate existing tables or insert duplicate data

-- 1. Create orders table if it doesn't exist
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

-- 2. Create performance indexes if they don't exist
CREATE INDEX IF NOT EXISTS idx_order_date ON orders(order_date);
CREATE INDEX IF NOT EXISTS idx_carrier ON orders(carrier);
CREATE INDEX IF NOT EXISTS idx_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_delivery_dates ON orders(promised_delivery_date, actual_delivery_date);
CREATE INDEX IF NOT EXISTS idx_region ON orders(destination_region);
CREATE INDEX IF NOT EXISTS idx_order_date_status ON orders(order_date, status);

-- 3. Add table and column comments
COMMENT ON TABLE orders IS 'Main orders table for logistics analytics dashboard';
COMMENT ON COLUMN orders.actual_delivery_date IS 'NULL means not yet delivered';
COMMENT ON COLUMN orders.status IS 'Valid values: pending, in_transit, delivered, cancelled';

-- 4. Insert sample data only if the table is empty
DO $$
BEGIN
    IF (SELECT COUNT(*) FROM orders) = 0 THEN
        RAISE NOTICE 'Table is empty, inserting sample data...';

        -- Insert 20,000 sample orders (adapted from sample-data.sql)
        INSERT INTO orders (
            order_date,
            promised_delivery_date,
            actual_delivery_date,
            status,
            carrier,
            destination_city,
            destination_state,
            destination_region,
            order_value,
            sku,
            quantity
        )
        WITH base_data AS (
            SELECT
                -- Order date: random date between 2024-01-01 and 2025-03-31
                '2024-01-01'::date + (random() * 825)::integer AS order_date,

                -- Promised delivery: 2-7 days after order date
                ('2024-01-01'::date + (random() * 825)::integer) + (2 + random() * 5)::integer AS promised_delivery_date,

                -- Actual delivery:
                CASE
                    -- 85% delivered, 10% in transit, 5% pending/cancelled
                    WHEN random() < 0.85 THEN
                        -- Delivered: sometimes on time, sometimes delayed
                        ('2024-01-01'::date + (random() * 825)::integer) +
                        (2 + random() * 5)::integer +
                        CASE WHEN random() < 0.2 THEN (1 + random() * 3)::integer ELSE 0 END
                    ELSE NULL
                END AS actual_delivery_date,

                -- Status
                CASE
                    WHEN random() < 0.85 THEN 'delivered'
                    WHEN random() < 0.95 THEN 'in_transit'
                    WHEN random() < 0.98 THEN 'pending'
                    ELSE 'cancelled'
                END AS status,

                -- Carrier: weighted distribution
                CASE
                    WHEN random() < 0.35 THEN 'UPS'
                    WHEN random() < 0.60 THEN 'FedEx'
                    WHEN random() < 0.80 THEN 'DHL'
                    WHEN random() < 0.90 THEN 'USPS'
                    WHEN random() < 0.95 THEN 'Amazon Logistics'
                    ELSE 'Regional Carrier'
                END AS carrier,

                -- Destination city
                (ARRAY['New York', 'Los Angeles', 'Chicago', 'Houston', 'Phoenix', 'Philadelphia',
                       'San Antonio', 'San Diego', 'Dallas', 'San Jose', 'Austin', 'Jacksonville',
                       'Fort Worth', 'Columbus', 'Charlotte', 'San Francisco', 'Indianapolis',
                       'Seattle', 'Denver', 'Boston'])[1 + (random() * 20)::integer] AS destination_city,

                -- Order value: $50-$5000, skewed toward lower values
                ROUND((50 + (random() * 4950 * (1 - random()^2)))::numeric, 2) AS order_value,

                -- SKU: random product codes
                'SKU-' || LPAD((1000 + (random() * 8999)::integer)::text, 4, '0') AS sku,

                -- Quantity: 1-50 items
                (1 + random() * 49)::integer AS quantity
            FROM generate_series(1, 20000)
        ),
        state_data AS (
            SELECT
                order_date,
                promised_delivery_date,
                actual_delivery_date,
                status,
                carrier,
                destination_city,
                -- Destination state (matching cities)
                CASE
                    WHEN destination_city IN ('New York', 'Buffalo', 'Rochester') THEN 'NY'
                    WHEN destination_city IN ('Los Angeles', 'San Diego', 'San Jose', 'San Francisco') THEN 'CA'
                    WHEN destination_city IN ('Chicago', 'Peoria', 'Springfield') THEN 'IL'
                    WHEN destination_city IN ('Houston', 'Dallas', 'San Antonio', 'Austin', 'Fort Worth') THEN 'TX'
                    WHEN destination_city IN ('Phoenix', 'Tucson', 'Mesa') THEN 'AZ'
                    WHEN destination_city IN ('Philadelphia', 'Pittsburgh', 'Allentown') THEN 'PA'
                    WHEN destination_city IN ('Jacksonville', 'Miami', 'Tampa') THEN 'FL'
                    WHEN destination_city IN ('Columbus', 'Cleveland', 'Cincinnati') THEN 'OH'
                    WHEN destination_city IN ('Charlotte', 'Raleigh', 'Greensboro') THEN 'NC'
                    WHEN destination_city IN ('Indianapolis', 'Fort Wayne', 'Evansville') THEN 'IN'
                    WHEN destination_city IN ('Seattle', 'Spokane', 'Tacoma') THEN 'WA'
                    WHEN destination_city IN ('Denver', 'Colorado Springs', 'Aurora') THEN 'CO'
                    WHEN destination_city IN ('Boston', 'Worcester', 'Springfield') THEN 'MA'
                    ELSE (ARRAY['GA', 'MI', 'TN', 'MO', 'MD', 'WI', 'MN', 'LA', 'AL', 'KY'])[1 + (random() * 10)::integer]
                END AS destination_state,
                order_value,
                sku,
                quantity
            FROM base_data
        )
        SELECT
            order_date,
            promised_delivery_date,
            actual_delivery_date,
            status,
            carrier,
            destination_city,
            destination_state,
            -- Destination region (based on state)
            CASE
                WHEN destination_state IN ('NY', 'NJ', 'PA', 'CT', 'MA', 'RI', 'NH', 'VT', 'ME') THEN 'Northeast'
                WHEN destination_state IN ('CA', 'OR', 'WA', 'NV', 'AZ', 'UT', 'CO', 'NM', 'WY', 'MT', 'ID') THEN 'West'
                WHEN destination_state IN ('TX', 'OK', 'AR', 'LA', 'MS', 'AL', 'GA', 'FL', 'SC', 'NC', 'TN', 'KY') THEN 'South'
                WHEN destination_state IN ('IL', 'IN', 'OH', 'MI', 'WI', 'MN', 'IA', 'MO', 'KS', 'NE', 'SD', 'ND') THEN 'Midwest'
                ELSE 'Other'
            END AS destination_region,
            order_value,
            sku,
            quantity
        FROM state_data;

        RAISE NOTICE 'Sample data inserted successfully.';
    ELSE
        RAISE NOTICE 'Table already contains data, skipping insertion.';
    END IF;
END $$;

-- 5. Verify the data (optional)
SELECT COUNT(*) AS total_orders FROM orders;
SELECT
    MIN(order_date) AS earliest_order,
    MAX(order_date) AS latest_order,
    COUNT(*) AS total_orders
FROM orders;