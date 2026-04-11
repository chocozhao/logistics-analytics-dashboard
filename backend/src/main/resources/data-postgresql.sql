-- Sample data generation for Logistics Analytics Dashboard
-- Generates 30,000+ realistic order records for development and testing
-- Date range: 2024-01-01 to 2026-04-05 (825 days)
-- Ensures continuous coverage for forecasting (especially Oct 2024 - Mar 2025)

-- Clear existing data (optional - uncomment for fresh database)
-- TRUNCATE TABLE orders RESTART IDENTITY;

-- Insert sample data with improved date consistency
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
        -- Order date: random date between 2024-01-01 and 2026-04-05 (825 days)
        -- Evenly distributed to ensure continuous time series
        '2024-01-01'::date + (random() * 825)::integer AS order_date,

        -- Promised delivery: 2-7 days after order date (consistent relationship)
        '2024-01-01'::date + (random() * 825)::integer + (2 + random() * 5)::integer AS promised_delivery_date,

        -- Status distribution: 85% delivered, 10% in transit, 5% pending/cancelled
        CASE
            WHEN random() < 0.85 THEN 'delivered'
            WHEN random() < 0.95 THEN 'in_transit'
            WHEN random() < 0.98 THEN 'pending'
            ELSE 'cancelled'
        END AS status,

        -- Carrier: weighted distribution (same as original)
        CASE
            WHEN random() < 0.35 THEN 'UPS'
            WHEN random() < 0.60 THEN 'FedEx'
            WHEN random() < 0.80 THEN 'DHL'
            WHEN random() < 0.90 THEN 'USPS'
            WHEN random() < 0.95 THEN 'Amazon Logistics'
            ELSE 'Regional Carrier'
        END AS carrier,

        -- Destination city from predefined list
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
    FROM generate_series(1, 30000)
),
processed_data AS (
    SELECT
        order_date,
        promised_delivery_date,
        -- Actual delivery date: only for delivered orders
        CASE
            WHEN status = 'delivered' THEN
                -- Sometimes on time, sometimes delayed (20% chance of delay)
                promised_delivery_date +
                CASE WHEN random() < 0.2 THEN (1 + random() * 3)::integer ELSE 0 END
            ELSE NULL
        END AS actual_delivery_date,
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
FROM processed_data;

-- Data quality verification (commented out for production)
/*
SELECT COUNT(*) AS total_orders FROM orders;
SELECT
    MIN(order_date) AS earliest_order,
    MAX(order_date) AS latest_order,
    COUNT(*) AS orders_in_forecast_range
FROM orders
WHERE order_date BETWEEN '2024-10-01' AND '2025-03-31';
SELECT status, COUNT(*) FROM orders GROUP BY status ORDER BY COUNT(*) DESC;
SELECT carrier, COUNT(*) FROM orders GROUP BY carrier ORDER BY COUNT(*) DESC;
SELECT destination_region, COUNT(*) FROM orders GROUP BY destination_region ORDER BY COUNT(*) DESC;

-- Sample time series check for forecasting
SELECT
    DATE_TRUNC('week', order_date) AS week,
    COUNT(*) AS order_count
FROM orders
WHERE order_date BETWEEN '2024-10-01' AND '2025-03-31'
GROUP BY DATE_TRUNC('week', order_date)
ORDER BY week;
*/